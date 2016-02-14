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
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGHelper;
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

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(REGIONS_ALIASES, key) && !map.containsKey("region")) {
            map.put("region", value);
        } else if (isIn(HANDLERS_ALIASES, key) && !map.containsKey("handler")) {
            map.put("handler", value);
        } else if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).parse();

        String worldName = parse.flagmap.get("world");
        World world = null;
        if (source instanceof Player) world = ((Player) source).getWorld();
        if (!worldName.isEmpty()) {
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
            if (optWorld.isPresent()) {
                world = optWorld.get();
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
        } else if (parse.args.length > 0 && parse.args.length < 3) {
            throw new CommandException(Text.of("Not enough arguments!"));
        } else if (parse.args.length == 3) {
            if (pPos == null)
                pPos = Vector3d.ZERO;
            try {
                x = FCHelper.parseCoordinate(pPos.getX(), parse.args[0]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[0] + "\"!"), e, parse.args[0], 0);
            }
            try {
                y = FCHelper.parseCoordinate(pPos.getY(), parse.args[1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[1] + "\"!"), e, parse.args[1], 1);
            }
            try {
                z = FCHelper.parseCoordinate(pPos.getZ(), parse.args[2]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(Text.of("Unable to parse \"" + parse.args[2] + "\"!"), e, parse.args[2], 2);
            }
        } else {
            throw new CommandException(Text.of("Too many arguments!"));
        }
        boolean flag = false;
        Text.Builder output = Text.builder();
        List<IRegion> regionList = FGManager.getInstance().getRegionList(world).stream()
                .filter(region -> region.isInRegion(x, y, z))
                .collect(Collectors.toList());
        output.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
        output.append(Text.of(TextColors.AQUA, "----- Position: (" + String.format("%.1f, %.1f, %.1f", x, y, z) + ") -----\n"));
        if (!parse.flagmap.containsKey("handler") || parse.flagmap.containsKey("region")) {
            output.append(Text.of(TextColors.GREEN, "------- Regions Located Here -------\n"));
            ListIterator<IRegion> regionListIterator = regionList.listIterator();
            while (regionListIterator.hasNext()) {
                IRegion region = regionListIterator.next();
                output.append(Text.of(FGHelper.getColorForObject(region),
                        TextActions.runCommand("/foxguard detail region --w:" + region.getWorld().getName() + " " + region.getName()),
                        TextActions.showText(Text.of("View Details")),
                        FGHelper.getRegionName(region, false)));
                if (regionListIterator.hasNext()) output.append(Text.of("\n"));
            }
            flag = true;
        }
        if (!parse.flagmap.containsKey("region") || parse.flagmap.containsKey("handler")) {
            if (flag) output.append(Text.of("\n"));
            List<IHandler> handlerList = new ArrayList<>();
            regionList.forEach(region -> region.getHandlers().stream()
                    .filter(handler -> !handlerList.contains(handler))
                    .forEach(handlerList::add));
            output.append(Text.of(TextColors.GREEN, "------- Handlers Located Here -------\n"));
            ListIterator<IHandler> handlerListIterator = handlerList.listIterator();
            while (handlerListIterator.hasNext()) {
                IHandler handler = handlerListIterator.next();
                output.append(Text.of(FGHelper.getColorForObject(handler),
                        TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                        TextActions.showText(Text.of("View Details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerListIterator.hasNext()) output.append(Text.of("\n"));
            }
        }
        source.sendMessage(output.build());
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT) && parse.current.index < 3 && parse.current.token.isEmpty()) {
            return ImmutableList.of(parse.current.prefix + "~");
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.SHORTFLAG)) {
            return ImmutableList.of("r", "h").stream()
                    .filter(flag -> !parse.flagmap.containsKey(flag))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world").stream()
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
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
