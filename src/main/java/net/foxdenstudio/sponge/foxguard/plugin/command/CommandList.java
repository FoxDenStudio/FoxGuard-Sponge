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
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

            Text.Builder builder = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Regions" + (allFlag ? "" : (" for World: \"" + worldName + "\"")) + " -------\n"));
            Collections.sort(regionList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            ListIterator<IRegion> regionListIterator = regionList.listIterator();
            while (regionListIterator.hasNext()) {
                IRegion region = regionListIterator.next();
                if (source instanceof Player) {
                    List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                    if (selectedRegions.contains(region)) {
                        builder.append(Text.of(TextColors.GRAY, "[+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s r remove " +
                                        FGUtil.genWorldFlag(region) +
                                        region.getName()),
                                TextActions.showText(Text.of("Remove from State Buffer")),
                                "[-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s r add " +
                                        FGUtil.genWorldFlag(region) +
                                        region.getName()),
                                TextActions.showText(Text.of("Add to State Buffer")),
                                "[+]"));
                        builder.append(Text.of(TextColors.GRAY, "[-]"));
                    }
                    builder.append(Text.of(" "));
                }
                builder.append(Text.of(FGUtil.getColorForObject(region),
                        TextActions.runCommand("/foxguard det r " + FGUtil.genWorldFlag(region) + region.getName()),
                        TextActions.showText(Text.of("View Details")),
                        FGUtil.getRegionName(region, allFlag)));
                if (regionListIterator.hasNext()) builder.append(Text.of("\n"));
            }
            source.sendMessage(builder.build());


        } else if (isIn(Aliases.HANDLERS_ALIASES, parse.args[0])) {
            boolean controllers = parse.flagmap.containsKey("all");
            List<IHandler> handlers = FGManager.getInstance().getHandlers(controllers).stream().collect(Collectors.toList());
            Text.Builder builder = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Handlers " + (controllers ? "and Controllers " : "") + "-------\n"));
            Collections.sort(handlers, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            Iterator<IHandler> handlerIterator = handlers.iterator();
            while (handlerIterator.hasNext()) {
                IHandler handler = handlerIterator.next();
                if (source instanceof Player) {
                    List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                    if (controllers) {
                        List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                        if (selectedHandlers.contains(handler)) {
                            builder.append(Text.of(TextColors.GRAY, "[h+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                                    TextActions.showText(Text.of("Remove from Handler State Buffer")),
                                    "[h-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + handler.getName()),
                                    TextActions.showText(Text.of("Add to Handler State Buffer")),
                                    "[h+]"));
                            builder.append(Text.of(TextColors.GRAY, "[h-]"));
                        }
                        if (handler instanceof IController) {
                            IController controller = ((IController) handler);
                            if (selectedControllers.contains(controller)) {
                                builder.append(Text.of(TextColors.GRAY, "[c+]"));
                                builder.append(Text.of(TextColors.RED,
                                        TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                        TextActions.showText(Text.of("Remove from Controller State Buffer")),
                                        "[c-]"));
                            } else {
                                builder.append(Text.of(TextColors.GREEN,
                                        TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                        TextActions.showText(Text.of("Add to Controller State Buffer")),
                                        "[c+]"));
                                builder.append(Text.of(TextColors.GRAY, "[c-]"));
                            }
                        } else {
                            builder.append(Text.of(TextColors.DARK_GRAY, "[c+][c-]"));
                        }
                    } else {
                        if (selectedHandlers.contains(handler)) {
                            builder.append(Text.of(TextColors.GRAY, "[+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                                    TextActions.showText(Text.of("Remove from State Buffer")),
                                    "[-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + handler.getName()),
                                    TextActions.showText(Text.of("Add to State Buffer")),
                                    "[+]"));
                            builder.append(Text.of(TextColors.GRAY, "[-]"));
                        }
                    }
                    builder.append(Text.of(" "));
                }
                builder.append(Text.of(FGUtil.getColorForObject(handler),
                        TextActions.runCommand("/foxguard det h " + handler.getName()),
                        TextActions.showText(Text.of("View Details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerIterator.hasNext()) builder.append(Text.of("\n"));
            }
            source.sendMessage(builder.build());
        } else if (isIn(Aliases.CONTROLLERS_ALIASES, parse.args[0])) {
            Set<IController> controllers = FGManager.getInstance().getControllers();
            Text.Builder output = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Controllers -------\n"));
            Iterator<IController> controllerIterator = controllers.iterator();
            while (controllerIterator.hasNext()) {
                IController controller = controllerIterator.next();
                if (source instanceof Player) {
                    List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                    List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                    if (selectedHandlers.contains(controller)) {
                        output.append(Text.of(TextColors.GRAY, "[h+]"));
                        output.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s h remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from Handler State Buffer")),
                                "[h-]"));
                    } else {
                        output.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s h add " + controller.getName()),
                                TextActions.showText(Text.of("Add to Handler State Buffer")),
                                "[h+]"));
                        output.append(Text.of(TextColors.GRAY, "[h-]"));
                    }
                    if (selectedControllers.contains(controller)) {
                        output.append(Text.of(TextColors.GRAY, "[c+]"));
                        output.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from Controller State Buffer")),
                                "[c-]"));
                    } else {
                        output.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                TextActions.showText(Text.of("Add to Controller State Buffer")),
                                "[c+]"));
                        output.append(Text.of(TextColors.GRAY, "[c-]"));
                    }
                    output.append(Text.of(" "));
                }
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
