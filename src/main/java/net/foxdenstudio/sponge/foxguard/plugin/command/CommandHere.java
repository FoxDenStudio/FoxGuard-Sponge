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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.CommandHUD;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.PlayerMoveListener;
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

public class CommandHere implements CommandCallable {

    private static final String[] PRIORITY_ALIASES = {"priority", "prio", "p"};

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(REGIONS_ALIASES, key) && !map.containsKey("region")) {
            map.put("region", value);
        } else if (isIn(HANDLERS_ALIASES, key) && !map.containsKey("handler")) {
            map.put("handler", value);
        } else if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(PRIORITY_ALIASES, key) && !map.containsKey("priority")) {
            map.put("priority", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();
        boolean hud = false;
        PlayerMoveListener.HUDConfig hudConfig = new PlayerMoveListener.HUDConfig(false, false, false);

        String worldName = parse.flags.get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
            } else {
                if (world == null)
                    throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
            }
        }
        if (world == null) throw new CommandException(Text.of("Must specify a world!"));
        double x, y, z;
        Vector3d pPos = null;
        if (source instanceof Player)
            pPos = ((Player) source).getLocation().getPosition();
        if (parse.args.length == 0) {
            if (pPos == null)
                throw new CommandException(Text.of("Must specify coordinates!"));
            x = pPos.getX();
            y = pPos.getY();
            z = pPos.getZ();
            hud = true;
        } else if (parse.args.length > 0 && parse.args.length < 3) {
            throw new CommandException(Text.of("Not enough arguments!"));
        } else if (parse.args.length == 3) {
            if (pPos == null)
                pPos = Vector3d.ZERO;
            try {
                x = FCUtil.parseCoordinate(pPos.getX(), parse.args[0]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[0] + "\"!"), e, parse.args[0], 0);
            }
            try {
                y = FCUtil.parseCoordinate(pPos.getY(), parse.args[1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[1] + "\"!"), e, parse.args[1], 1);
            }
            try {
                z = FCUtil.parseCoordinate(pPos.getZ(), parse.args[2]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[2] + "\"!"), e, parse.args[2], 2);
            }
        } else {
            throw new CommandException(Text.of("Too many arguments!"));
        }
        boolean flag = false;
        Text.Builder output = Text.builder();
        final World finalWorld = world;
        List<IRegion> regionList = FGManager.getInstance().getAllRegions(world).stream()
                .filter(region -> region.contains(x, y, z, finalWorld))
                .collect(Collectors.toList());
        List<IHandler> handlerList = new ArrayList<>();
        output.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
        output.append(Text.of(TextColors.AQUA, "----- Position: (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ") -----\n"));
        if (!parse.flags.containsKey("handler") || parse.flags.containsKey("region")) {
            output.append(Text.of(TextColors.GREEN, "------- Regions Located Here -------\n"));
            Collections.sort(regionList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            ListIterator<IRegion> regionListIterator = regionList.listIterator();
            while (regionListIterator.hasNext()) {
                IRegion region = regionListIterator.next();
                if (source instanceof Player) {
                    List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                    if (selectedRegions.contains(region)) {
                        output.append(Text.of(TextColors.GRAY, "[+]"));
                        output.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s r remove " +
                                        (region instanceof IWorldRegion ? ("--w:" + ((IWorldRegion) region).getWorld() + " ") : "") +
                                        region.getName()),
                                TextActions.showText(Text.of("Remove from State Buffer")),
                                "[-]"));
                    } else {
                        output.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s r add " +
                                        (region instanceof IWorldRegion ? ("--w:" + ((IWorldRegion) region).getWorld().getName() + " ") : "") +
                                        region.getName()),
                                TextActions.showText(Text.of("Add to State Buffer")),
                                "[+]"));
                        output.append(Text.of(TextColors.GRAY, "[-]"));
                    }
                    output.append(Text.of(" "));
                }
                output.append(Text.of(FGUtil.getColorForObject(region),
                        TextActions.runCommand("/foxguard detail region" + (region instanceof IWorldRegion ? (" --w:" + ((IWorldRegion) region).getWorld().getName() + " ") : "") + region.getName()),
                        TextActions.showText(Text.of("View Details")),
                        FGUtil.getRegionName(region, false)));
                if (regionListIterator.hasNext()) output.append(Text.of("\n"));
            }
            flag = true;
            hudConfig.regions = true;
        }
        if (!parse.flags.containsKey("region") || parse.flags.containsKey("handler")) {
            if (flag) output.append(Text.of("\n"));

            regionList.forEach(region -> region.getHandlers().stream()
                    .filter(handler -> !handlerList.contains(handler))
                    .forEach(handlerList::add));
            output.append(Text.of(TextColors.GREEN, "------- Handlers Located Here -------\n"));
            if (parse.flags.containsKey("priority")) {
                Collections.sort(handlerList, (o1, o2) -> o2.getPriority() - o1.getPriority());
                hudConfig.priority = true;
            } else {
                Collections.sort(handlerList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            }
            ListIterator<IHandler> handlerListIterator = handlerList.listIterator();
            while (handlerListIterator.hasNext()) {
                IHandler handler = handlerListIterator.next();
                if (source instanceof Player) {
                    List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                    List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                    if (selectedHandlers.contains(handler)) {
                        output.append(Text.of(TextColors.GRAY, "[h+]"));
                        output.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                                TextActions.showText(Text.of("Remove from Handler State Buffer")),
                                "[h-]"));
                    } else {
                        output.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s h add " + handler.getName()),
                                TextActions.showText(Text.of("Add to Handler State Buffer")),
                                "[h+]"));
                        output.append(Text.of(TextColors.GRAY, "[h-]"));
                    }
                    if (handler instanceof IController) {
                        IController controller = ((IController) handler);
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
                    } else {
                        output.append(Text.of(TextColors.DARK_GRAY, "[c+][c-]"));
                    }
                    output.append(Text.of(" "));
                }
                output.append(Text.of(FGUtil.getColorForObject(handler),
                        TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                        TextActions.showText(Text.of("View Details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerListIterator.hasNext()) output.append(Text.of("\n"));
            }
            hudConfig.handlers = true;
        }
        source.sendMessage(output.build());
        if (hud) {
            Player player = (Player) source;
            if (CommandHUD.instance().getIsHUDEnabled().get(player)) {
                PlayerMoveListener instance = PlayerMoveListener.getInstance();
                instance.getHudConfigMap().put(player, hudConfig);
                instance.renderHUD(player, regionList, handlerList, hudConfig);
                instance.showScoreboard(player);
            }
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT) && parse.current.index < 3 && parse.current.token.isEmpty()) {
            return ImmutableList.of(parse.current.prefix + "~");
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.SHORTFLAG)) {
            return ImmutableList.of("r", "h", "p").stream()
                    .filter(flag -> !parse.flags.containsKey(flag))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world", "regions", "handlers", "priority").stream()
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
        return source.hasPermission("foxguard.command.info.here");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("here [-r] [-h] [--w:<world>] [<x> <y> <z>]");
    }
}
