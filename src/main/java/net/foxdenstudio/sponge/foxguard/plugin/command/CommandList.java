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
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
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
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandList extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(ALL_ALIASES, key) && !map.containsKey("all")) {
            map.put("all", value);
        } else if (isIn(SUPER_ALIASES, key) && !map.containsKey("super")) {
            map.put("super", value);
        } else if (isIn(QUERY_ALIASES, key) && !map.containsKey("query")) {
            map.put("query", value);
        } else if (isIn(PAGE_ALIASES, key) && !map.containsKey("page")) {
            map.put("page", value);
        } else if (isIn(NUMBER_ALIASES, key) && !map.containsKey("number")) {
            map.put("number", value);
        }
        return true;
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
            String worldName = parse.flags.get("world");
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    regionList.addAll(FGManager.getInstance().getWorldRegions(optWorld.get()));
                    if (parse.flags.containsKey("super"))
                        regionList.addAll(FGManager.getInstance().getRegions());
                    allFlag = false;
                }
            }
            if (allFlag) {
                if (parse.flags.containsKey("super")) regionList.addAll(FGManager.getInstance().getRegions());
                else regionList.addAll(FGManager.getInstance().getAllRegions());
            }
            if (parse.flags.containsKey("query")) {
                String query = parse.flags.get("query");
                if (query.startsWith("/")) {
                    FCCUtil.FCPattern pattern = FCCUtil.parseUserRegex(query);
                    regionList = regionList.stream()
                            .filter(region -> pattern.matches(region.getName()))
                            .collect(Collectors.toList());
                } else {
                    regionList = regionList.stream()
                            .filter(region -> region.getName().toLowerCase().contains(query.toLowerCase()))
                            .collect(Collectors.toList());
                }
            }

            int page, number;
            if (parse.flags.containsKey("page")) {
                try {
                    page = Integer.parseInt(parse.flags.get("page"));
                } catch (NumberFormatException ignored) {
                    page = 1;
                }
            } else page = 1;
            if (parse.flags.containsKey("number")) {
                try {
                    number = Integer.parseInt(parse.flags.get("number"));
                } catch (NumberFormatException ignored) {
                    if (source instanceof Player) number = 18;
                    else number = Integer.MAX_VALUE;
                }
            } else {
                if (source instanceof Player) number = 18;
                else number = Integer.MAX_VALUE;
            }
            if (number < 1) number = 1;
            int maxPage = (Math.max(regionList.size() - 1, 0) / number) + 1;
            if (page < 1) page = 1;
            else if (page > maxPage) page = maxPage;
            int skip = (page - 1) * number;

            Text.Builder builder = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Regions" + (allFlag ? "" : (" for World: \"" + worldName + "\"")) + " -------\n"));
            Collections.sort(regionList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            Iterator<IRegion> regionIterator = regionList.iterator();
            for (int i = 0; i < skip; i++) {
                regionIterator.next();
            }
            int count = 0;
            while (regionIterator.hasNext() && count < number) {
                IRegion region = regionIterator.next();
                if (source instanceof Player) {
                    List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                    if (selectedRegions.contains(region)) {
                        builder.append(Text.of(TextColors.GRAY, "[+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s r remove " +
                                        FGUtil.genWorldFlag(region) +
                                        region.getName()),
                                TextActions.showText(Text.of("Remove from state buffer")),
                                "[-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s r add " +
                                        FGUtil.genWorldFlag(region) +
                                        region.getName()),
                                TextActions.showText(Text.of("Add to state buffer")),
                                "[+]"));
                        builder.append(Text.of(TextColors.GRAY, "[-]"));
                    }
                    builder.append(Text.of(" "));
                }
                builder.append(Text.of(FGUtil.getColorForObject(region),
                        TextActions.runCommand("/foxguard det r " + FGUtil.genWorldFlag(region) + region.getName()),
                        TextActions.showText(Text.of("View details")),
                        FGUtil.getRegionDisplayName(region, allFlag)));
                count++;
                if (regionIterator.hasNext() && count < number) builder.append(Text.NEW_LINE);
            }
            if (maxPage > 1)
                builder.append(Text.NEW_LINE).append(FCPUtil.pageFooter(page, maxPage, "/fg ls " + arguments, null));
            source.sendMessage(builder.build());

            //----------------------------------------------------------------------------------------------------------
        } else if (isIn(Aliases.HANDLERS_ALIASES, parse.args[0])) {
            boolean controllers = parse.flags.containsKey("all");
            List<IHandler> handlerList = new ArrayList<>(FGManager.getInstance().getHandlers(controllers));
            if (parse.flags.containsKey("query")) {
                String query = parse.flags.get("query");
                if (query.startsWith("/")) {
                    FCCUtil.FCPattern pattern = FCCUtil.parseUserRegex(query);
                    handlerList = handlerList.stream()
                            .filter(handler -> pattern.matches(handler.getName()))
                            .collect(Collectors.toList());
                } else {
                    handlerList = handlerList.stream()
                            .filter(handler -> handler.getName().toLowerCase().contains(query.toLowerCase()))
                            .collect(Collectors.toList());
                }
            }

            int page, number;
            if (parse.flags.containsKey("page")) {
                try {
                    page = Integer.parseInt(parse.flags.get("page"));
                } catch (NumberFormatException ignored) {
                    page = 1;
                }
            } else page = 1;
            if (parse.flags.containsKey("number")) {
                try {
                    number = Integer.parseInt(parse.flags.get("number"));
                } catch (NumberFormatException ignored) {
                    if (source instanceof Player) number = 18;
                    else number = Integer.MAX_VALUE;
                }
            } else {
                if (source instanceof Player) number = 18;
                else number = Integer.MAX_VALUE;
            }
            if (number < 1) number = 1;
            int maxPage = (Math.max(handlerList.size() - 1, 0) / number) + 1;
            if (page < 1) page = 1;
            else if (page > maxPage) page = maxPage;
            int skip = (page - 1) * number;

            Text.Builder builder = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Handlers " + (controllers ? "and Controllers " : "") + "-------\n"));
            Collections.sort(handlerList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            Iterator<IHandler> handlerIterator = handlerList.iterator();
            for (int i = 0; i < skip; i++) {
                handlerIterator.next();
            }
            int count = 0;
            while (handlerIterator.hasNext() && count < number) {
                IHandler handler = handlerIterator.next();
                if (source instanceof Player) {
                    List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                    if (controllers) {
                        List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                        if (selectedHandlers.contains(handler)) {
                            builder.append(Text.of(TextColors.GRAY, "[h+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                                    TextActions.showText(Text.of("Remove from handler state buffer")),
                                    "[h-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + handler.getName()),
                                    TextActions.showText(Text.of("Add to handler state buffer")),
                                    "[h+]"));
                            builder.append(Text.of(TextColors.GRAY, "[h-]"));
                        }
                        if (handler instanceof IController) {
                            IController controller = ((IController) handler);
                            if (selectedControllers.contains(controller)) {
                                builder.append(Text.of(TextColors.GRAY, "[c+]"));
                                builder.append(Text.of(TextColors.RED,
                                        TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                        TextActions.showText(Text.of("Remove from controller state buffer")),
                                        "[c-]"));
                            } else {
                                builder.append(Text.of(TextColors.GREEN,
                                        TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                        TextActions.showText(Text.of("Add to controller state buffer")),
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
                                    TextActions.showText(Text.of("Remove from state buffer")),
                                    "[-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + handler.getName()),
                                    TextActions.showText(Text.of("Add to state buffer")),
                                    "[+]"));
                            builder.append(Text.of(TextColors.GRAY, "[-]"));
                        }
                    }
                    builder.append(Text.of(" "));
                }
                builder.append(Text.of(FGUtil.getColorForObject(handler),
                        TextActions.runCommand("/foxguard det h " + handler.getName()),
                        TextActions.showText(Text.of("View details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerIterator.hasNext() && count < number) builder.append(Text.NEW_LINE);
            }
            if (maxPage > 1)
                builder.append(Text.NEW_LINE).append(FCPUtil.pageFooter(page, maxPage, "/fg ls " + arguments, null));
            source.sendMessage(builder.build());

            //----------------------------------------------------------------------------------------------------------
        } else if (isIn(Aliases.CONTROLLERS_ALIASES, parse.args[0])) {
            List<IController> controllerList = new ArrayList<>(FGManager.getInstance().getControllers());
            if (parse.flags.containsKey("query")) {
                String query = parse.flags.get("query");
                if (query.startsWith("/")) {
                    FCCUtil.FCPattern pattern = FCCUtil.parseUserRegex(query);
                    controllerList = controllerList.stream()
                            .filter(controller -> pattern.matches(controller.getName()))
                            .collect(Collectors.toList());
                } else {
                    controllerList = controllerList.stream()
                            .filter(controller -> controller.getName().toLowerCase().contains(query.toLowerCase()))
                            .collect(Collectors.toList());
                }
            }

            int page, number;
            if (parse.flags.containsKey("page")) {
                try {
                    page = Integer.parseInt(parse.flags.get("page"));
                } catch (NumberFormatException ignored) {
                    page = 1;
                }
            } else page = 1;
            if (parse.flags.containsKey("number")) {
                try {
                    number = Integer.parseInt(parse.flags.get("number"));
                } catch (NumberFormatException ignored) {
                    if (source instanceof Player) number = 18;
                    else number = Integer.MAX_VALUE;
                }
            } else {
                if (source instanceof Player) number = 18;
                else number = Integer.MAX_VALUE;
            }
            if (number < 1) number = 1;
            int maxPage = (Math.max(controllerList.size() - 1, 0) / number) + 1;
            if (page < 1) page = 1;
            else if (page > maxPage) page = maxPage;
            int skip = (page - 1) * number;

            Text.Builder builder = Text.builder()
                    .append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"))
                    .append(Text.of(TextColors.GREEN, "------- Controllers -------\n"));
            Iterator<IController> controllerIterator = controllerList.iterator();
            for (int i = 0; i < skip; i++) {
                controllerIterator.next();
            }
            int count = 0;
            while (controllerIterator.hasNext() && count < number) {
                IController controller = controllerIterator.next();
                if (source instanceof Player) {
                    List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                    List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                    if (selectedHandlers.contains(controller)) {
                        builder.append(Text.of(TextColors.GRAY, "[h+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s h remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from handler state buffer")),
                                "[h-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s h add " + controller.getName()),
                                TextActions.showText(Text.of("Add to handler state buffer")),
                                "[h+]"));
                        builder.append(Text.of(TextColors.GRAY, "[h-]"));
                    }
                    if (selectedControllers.contains(controller)) {
                        builder.append(Text.of(TextColors.GRAY, "[c+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from controller state buffer")),
                                "[c-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                TextActions.showText(Text.of("Add to controller state buffer")),
                                "[c+]"));
                        builder.append(Text.of(TextColors.GRAY, "[c-]"));
                    }
                    builder.append(Text.of(" "));
                }
                builder.append(Text.of(FGUtil.getColorForObject(controller),
                        TextActions.runCommand("/foxguard det h " + controller.getName()),
                        TextActions.showText(Text.of("View details")),
                        controller.getShortTypeName() + " : " + controller.getName()));
                count++;
                if (controllerIterator.hasNext() && count < number) builder.append(Text.NEW_LINE);
            }
            if (maxPage > 1)
                builder.append(Text.NEW_LINE).append(FCPUtil.pageFooter(page, maxPage, "/fg ls " + arguments, null));

            source.sendMessage(builder.build());
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
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
                return Stream.of("regions", "handlers", "controllers")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
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
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.info.list");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("Lists the regions/handlers on this server"));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("list <regions [--w:<world>] | handlers>");
    }
}
