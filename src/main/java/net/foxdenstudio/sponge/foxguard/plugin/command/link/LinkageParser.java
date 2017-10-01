/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxguard.plugin.command.link;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Fox on 4/19/2016.
 */
public final class LinkageParser {

    private static final String REGEX = "[>()]|([\"'])(?:\\\\.|[^\\\\>(),])*?\\1|(?:\\\\.|[^\"'\\s>(),])+";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private World currentWorld;

    private LinkageParser(CommandSource source) {
        if (source instanceof Locatable) {
            currentWorld = ((Locatable) source).getWorld();
        }
    }

    public static Set<LinkEntry> parseLinkageExpression(String expression, CommandSource source) throws CommandException {
        return new LinkageParser(source).parse(expression, source);
    }

    public static List<String> getSuggestions(String expressionString, CommandSource source) {
        String[] parts = expressionString.split(" +", -1);
        String endPart = parts[parts.length - 1];
        Matcher matcher = PATTERN.matcher(expressionString);
        boolean found = false;
        Stage stage = Stage.START;
        World world = source instanceof Locatable ? ((Locatable) source).getWorld() : null;
        int parentheses = 0;
        String match = "";
        while (matcher.find()) {
            found = true;
            match = matcher.group();
            if (match.equals("(")) parentheses++;
            else if (match.equals(")")) parentheses--;
            else if (match.equals(">") && parentheses == 0) stage = Stage.REST;
            else if (match.startsWith("%")) {
                Optional<World> worldOptional = Sponge.getServer().getWorld(match.substring(1));
                if (worldOptional.isPresent()) world = worldOptional.get();
            }
            if (parentheses < 0) return ImmutableList.of();
        }
        if (found) {
            String token = match;
            if (expressionString.endsWith(">") || expressionString.endsWith(",")) {
                return ImmutableList.of(endPart + " ");
            } else if (expressionString.endsWith(" ")) {
                if (stage == Stage.START) {
                    return FGManager.getInstance().getAllRegions(world).stream()
                            .map(IFGObject::getName)
                            .sorted()
                            .collect(Collectors.toList());
                } else {
                    return FGManager.getInstance().getHandlers().stream()
                            .map(IFGObject::getName)
                            .sorted()
                            .collect(Collectors.toList());
                }
            } else if (expressionString.endsWith(token)) {
                if (token.startsWith("^")) {
                    return FGManager.getInstance().getControllers().stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(token.substring(1)))
                            .sorted()
                            .map(str -> endPart + str.substring(token.length() - 1))
                            .collect(Collectors.toList());
                } else if (token.startsWith("%")) {
                    return Sponge.getServer().getWorlds().stream()
                            .map(World::getName)
                            .filter(new StartsWithPredicate(token.substring(1)))
                            .map(str -> endPart + str.substring(token.length() - 1))
                            .collect(Collectors.toList());
                } else if (token.startsWith("$")) {
                    return Stream.of("regions", "handlers", "controllers")
                            .filter(new StartsWithPredicate(token.substring(1)))
                            .map(str -> endPart + str.substring(token.length() - 1))
                            .collect(Collectors.toList());
                } else {
                    if (stage == Stage.START) {
                        return FGManager.getInstance().getAllRegions(world).stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(token))
                                .sorted()
                                .map(str -> endPart + str.substring(token.length()))
                                .collect(Collectors.toList());
                    } else {
                        return FGManager.getInstance().getHandlers().stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(token))
                                .sorted()
                                .map(str -> endPart + str.substring(token.length()))
                                .collect(Collectors.toList());
                    }
                }
            }
        } else {
            return FGManager.getInstance().getAllRegions(world).stream()
                    .map(IFGObject::getName)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return ImmutableList.of();
    }

    private static boolean checkParentheses(String expression) {
        Pattern leftPattern = Pattern.compile("\\(");
        Matcher leftMatcher = leftPattern.matcher(expression);
        int leftCount = 0;
        while (leftMatcher.find()) {
            leftCount++;
        }
        Pattern rightPattern = Pattern.compile("\\)");
        Matcher rightMatcher = rightPattern.matcher(expression);
        int rightCount = 0;
        while (rightMatcher.find()) {
            rightCount++;
        }
        return leftCount == rightCount;
    }

    private Set<LinkEntry> parse(String expressionString, CommandSource source) throws CommandException {
        String[] parts = expressionString.split(";");
        Set<LinkEntry> set = new LinkedHashSet<>();
        for (String part : parts) {
            if (!checkParentheses(part)) {
                throw new CommandException(Text.of("You must close all parentheses!"));
            }
            IExpression expression = new Expression(part, source, Stage.START);
            set.addAll(expression.getLinks());
        }
        return ImmutableSet.copyOf(set);
    }

    private enum Stage {
        START, REST
    }

    public class Expression implements IExpression {

        private List<Set<IExpression>> contents = new ArrayList<>();
        private Stage stage;

        public Expression(String expressionString, CommandSource source, Stage stage) {
            this.stage = stage;
            Matcher matcher = PATTERN.matcher(expressionString);
            int parentheses = 0;
            int startIndex = 0;
            while (matcher.find()) {
                if (matcher.group().equals("(")) parentheses++;
                else if (matcher.group().equals(")")) parentheses--;
                else if (matcher.group().equals(">") && parentheses == 0) {
                    contents.add(parseSegment(expressionString.substring(startIndex, matcher.start()), source));
                    startIndex = matcher.end();
                    this.stage = Stage.REST;
                }
            }
            contents.add(parseSegment(expressionString.substring(startIndex, expressionString.length()), source));
        }

        private Set<IExpression> parseSegment(String segmentString, CommandSource source) {
            Set<IExpression> set = new LinkedHashSet<>();
            Set<IFGObject> stubObjects = new LinkedHashSet<>();
            Matcher matcher = PATTERN.matcher(segmentString);
            while (matcher.find()) {
                if (matcher.group().equals("(")) {
                    int startIndex = matcher.end();
                    int parentheses = 1;
                    while (parentheses != 0 && matcher.find()) {
                        if (matcher.group().equals("(")) parentheses++;
                        else if (matcher.group().equals(")")) parentheses--;
                    }
                    set.add(new Expression(segmentString.substring(startIndex, matcher.start()), source, stage));
                } else {
                    String token = matcher.group();
                    if (!token.startsWith("-")) {
                        if (token.startsWith("%")) {
                            Optional<World> worldOptional = Sponge.getServer().getWorld(token.substring(1));
                            worldOptional.ifPresent(world -> currentWorld = world);
                        } else if (token.startsWith("$")) {
                            String name = token.substring(1);
                            if (Aliases.isIn(Aliases.REGIONS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedRegions(source))));
                            } else if (Aliases.isIn(Aliases.HANDLERS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedHandlers(source))));
                            } else if (Aliases.isIn(Aliases.CONTROLLERS_ALIASES, name)) {
                                set.add(new ExpressionStub(ImmutableSet.copyOf(FGUtil.getSelectedControllers(source))));
                            }
                        } else if (token.startsWith("^")) {
                            try {
                                FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(token.substring(1));
                                Optional<IController> controllerOpt = FGManager.getInstance().getController(ownerResult.getName(), ownerResult.getOwner());
                                controllerOpt.ifPresent(stubObjects::add);
                            } catch (CommandException ignored) {
                            }
                        } else if (stage == Stage.START) {
                            try {
                                FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(token);
                                Optional<IRegion> regionOpt = FGManager.getInstance().getRegionFromWorld(currentWorld, ownerResult.getName(), ownerResult.getOwner());
                                regionOpt.ifPresent(stubObjects::add);
                            } catch (CommandException ignored) {
                            }
                        } else {
                            try {
                                FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(token);
                                Optional<IHandler> handlerOpt = FGManager.getInstance().getHandler(ownerResult.getName(), ownerResult.getOwner());
                                handlerOpt.ifPresent(stubObjects::add);
                            } catch (CommandException ignored) {
                            }
                        }
                    }
                }
            }
            if (stubObjects.size() > 0) set.add(new ExpressionStub(stubObjects));
            return ImmutableSet.copyOf(set);
        }

        @Override
        public Set<IFGObject> getValue() {
            if (contents.size() > 0) {
                Set<IFGObject> set = new LinkedHashSet<>();
                for (IExpression expression : contents.get(0)) {
                    set.addAll(expression.getValue());
                }
                return set;
            } else return ImmutableSet.of();
        }

        @Override
        public Set<LinkEntry> getLinks() {
            if (contents.size() > 0) {
                Set<LinkEntry> set = new LinkedHashSet<>();
                if (contents.size() > 1) {
                    for (Set<IExpression> eSet : contents) {
                        for (IExpression ex : eSet) {
                            set.addAll(ex.getLinks());
                        }
                    }
                    Set<IExpression> from, to = contents.get(0);
                    for (int i = 1; i < contents.size(); i++) {
                        from = to;
                        to = contents.get(i);
                        for (IExpression fromEx : from) {
                            for (IExpression toEx : to) {
                                fromEx.getValue().stream()
                                        .filter(fromObj -> fromObj instanceof ILinkable)
                                        .forEach(fromObj -> set.addAll(toEx.getValue().stream()
                                                .filter(toObj -> toObj instanceof IHandler)
                                                .map(toObj -> new LinkEntry((ILinkable) fromObj, (IHandler) toObj))
                                                .collect(Collectors.toList())));
                            }
                        }
                    }
                    return set;
                } else {
                    for (IExpression e : contents.get(0)) {
                        set.addAll(e.getLinks());
                    }
                    return set;
                }
            } else return ImmutableSet.of();
        }

    }

    public class Result {
        public Set<LinkEntry> entries = new HashSet<>();
        public Map<String, String> flags = new CacheMap<>((k, m) -> "");
    }

    public class ExpressionStub implements IExpression {

        Set<IFGObject> set;

        public ExpressionStub(Set<IFGObject> set) {
            this.set = set;
        }

        public Set<IFGObject> getValue() {
            return set;
        }

        @Override
        public Set<LinkEntry> getLinks() {
            return ImmutableSet.of();
        }

    }
}
