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

package net.foxdenstudio.sponge.foxguard.plugin.command;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandModify extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
        return true;
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else {
            String category = parse.args[0];
            FGCat fgCat = FGCat.from(category);
            if (fgCat == null) throw new CommandException(Text.of("\"" + category + "\" is not a valid category!"));

            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(parse.args[1]);

            IFGObject object;
            switch (fgCat) {
                case REGION:
                    object = FGUtil.getRegionFromCommand(source, ownerResult, parse.flags.containsKey("world"), parse.flags.get("world"));
                    if (object instanceof IWorldRegion) fgCat = FGCat.WORLDREGION;
                    break;
                case HANDLER:
                    object = FGUtil.getHandlerFromCommand(ownerResult);
                    if (object instanceof IController) fgCat = FGCat.CONTROLLER;
                    break;
                default:
                    throw new CommandException(Text.of("Something went horribly wrong."));
            }

            ProcessResult result = object.modify(source, parse.args.length < 3 ? "" : parse.args[2]);
            Optional<Text> messageOptional = result.getMessage();
            boolean success = result.isSuccess();
            TextColor color;
            if (success) {
                color = TextColors.GREEN;
                FGUtil.markDirty(object);
            } else {
                color = TextColors.RED;
            }

            if (messageOptional.isPresent()) {
                if (!FCPUtil.hasColor(messageOptional.get())) {
                    source.sendMessage(messageOptional.get().toBuilder().color(color).build());
                } else {
                    source.sendMessage(messageOptional.get());
                }
            } else {
                source.sendMessage(Text.of(color, success ?
                        "Successfully modified " + fgCat.lName + "!" :
                        "Modification failed for " + fgCat.lName + "!"));
            }

            return CommandResult.empty();
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Stream.of("region", "handler")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 1) {
                FGUtil.OwnerTabResult result = FGUtil.getOwnerSuggestions(parse.current.token);
                if (result.isComplete()) {
                    return result.getSuggestions().stream()
                            .map(str -> parse.current.prefix + str)
                            .collect(GuavaCollectors.toImmutableList());
                }

                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    boolean key = parse.flags.containsKey("world");
                    World world = null;
                    if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (key && world != null) {
                        return FGManager.getInstance().getAllRegions(world, result.getOwner()).stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else {
                        return FGManager.getInstance().getAllRegionsWithUniqueNames(result.getOwner(), world).stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers(result.getOwner()).stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(result.getToken()))
                            .map(args -> parse.current.prefix + result.getPrefix() + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return Stream.of("world")
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.FINAL)) {
            if (isIn(REGIONS_ALIASES, parse.args[0])) {
                IRegion region = FGManager.getInstance().getRegion(parse.args[1]).orElse(null);
                if (region == null) {
                    String worldName = parse.flags.get("world");
                    World world = null;
                    if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        } else return ImmutableList.of();
                    }
                    if (world == null) return ImmutableList.of();
                    region = FGManager.getInstance().getWorldRegion(world, parse.args[1]).orElse(null);
                }

                if (region == null) return ImmutableList.of();
                List<String> suggestions = region.modifySuggestions(source, parse.current.token, null);
                if (suggestions == null) return ImmutableList.of();
                return suggestions.stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                if (parse.args.length < 2) return ImmutableList.of();
                Optional<IHandler> handlerOpt = FGManager.getInstance().getHandler(parse.args[1]);
                if (!handlerOpt.isPresent()) return ImmutableList.of();
                List<String> suggestions = handlerOpt.get().modifySuggestions(source, parse.current.token, null);
                if (suggestions == null) return ImmutableList.of();
                return suggestions.stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.modify");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("modify <region [--w:<worldname>] | handler> <name> [args...]");
    }

    private enum FGCat {
        REGION(REGIONS_ALIASES),
        WORLDREGION(null),
        HANDLER(HANDLERS_ALIASES),
        CONTROLLER(null);

        public final String[] catAliases;
        public final String lName = name().toLowerCase();

        FGCat(String[] catAliases) {
            this.catAliases = catAliases;

        }

        public static FGCat from(String category) {
            for (FGCat cat : values()) {
                if (isIn(cat.catAliases, category)) return cat;
            }
            return null;
        }
    }
}
