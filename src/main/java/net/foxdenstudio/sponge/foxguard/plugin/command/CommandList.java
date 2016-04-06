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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandList implements CommandCallable {

    private static final String[] SUPER_ALIASES = {"super", "sup", "s"};

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (Aliases.isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (Aliases.isIn(ALL_ALIASES, key) && !map.containsKey("all")) {
            map.put("all", value);
        } else if (isIn(SUPER_ALIASES, key) && !map.containsKey("super")) {
            map.put("super", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isIn(Aliases.REGIONS_ALIASES, parse.args[0])) {
            List<IRegion> regionList = new ArrayList<>();
            boolean allFlag = true;
            String worldName = parse.flagmap.get("world");
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    FGManager.getInstance().getWorldRegions(optWorld.get()).forEach(regionList::add);
                    allFlag = false;
                }
            }
            if (allFlag) {
                FGManager.getInstance().getAllRegions().forEach(regionList::add);
            }

            Text.Builder output = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Regions" + (allFlag ? "" : (" for World: \"" + worldName + "\"")) + " -------\n"));
            ListIterator<IRegion> regionListIterator = regionList.listIterator();
            while (regionListIterator.hasNext()) {
                IRegion region = regionListIterator.next();
                output.append(Text.of(FGUtil.getColorForObject(region),
                        TextActions.runCommand("/foxguard det r " + FGUtil.genWorldFlag(region) + region.getName()),
                        TextActions.showText(Text.of("View Details")),
                        FGUtil.getRegionName(region, allFlag)));
                if (regionListIterator.hasNext()) output.append(Text.of("\n"));
            }
            source.sendMessage(output.build());


        } else if (isIn(Aliases.HANDLERS_ALIASES, parse.args[0])) {
            boolean controllers = parse.flagmap.containsKey("all");
            Set<IHandler> handlers = FGManager.getInstance().getHandlers(controllers);
            Text.Builder output = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Handlers " + (controllers ? "and Controllers " : "") + "-------\n"));
            Iterator<IHandler> handlerIterator = handlers.iterator();
            while (handlerIterator.hasNext()) {
                IHandler handler = handlerIterator.next();
                output.append(Text.of(FGUtil.getColorForObject(handler),
                        TextActions.runCommand("/foxguard det h " + handler.getName()),
                        TextActions.showText(Text.of("View Details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerIterator.hasNext()) output.append(Text.of("\n"));
            }
            source.sendMessage(output.build());
        } else if (isIn(Aliases.CONTROLLERS_ALIASES, parse.args[0])) {
            Set<IController> controllers = FGManager.getInstance().getControllers();
            Text.Builder output = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Controllers -------\n"));
            Iterator<IController> controllerIterator = controllers.iterator();
            while (controllerIterator.hasNext()) {
                IController controller = controllerIterator.next();
                output.append(Text.of(FGUtil.getColorForObject(controller),
                        TextActions.runCommand("/foxguard det h " + controller.getName()),
                        TextActions.showText(Text.of("View Details")),
                        controller.getShortTypeName() + " : " + controller.getName()));
                if (controllerIterator.hasNext()) output.append(Text.of("\n"));
            }
            source.sendMessage(output.build());
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Arrays.asList(FGManager.TYPES).stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world").stream()
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
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.info.list");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("Lists the regions/handlers on this server"));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("list <regions [--w:<world>] | handlers>");
    }
}
