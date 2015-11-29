/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.commands;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.*;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandAdd implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (isAlias(REGIONS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FGManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new ArgumentParseException(Texts.of("No Regions with this name!"),
                            arguments, args[0].length() + (flag == 1 ? args[1].length() + 1 : 0) + args[1 + flag].length() / 2);
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.contains(region))
                    throw new ArgumentParseException(Texts.of("Region is already in your state buffer!"), args[1 + flag], 1 + flag);
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.add(region);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added Region to your state buffer!"));
                return CommandResult.success();
            } else if (isAlias(HANDLERS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                IHandler handler = FGManager.getInstance().gethandler(args[1]);
                if (handler == null)
                    throw new ArgumentParseException(Texts.of("No Handlers with this name!"), args[1], 1);
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedHandlers.contains(handler))
                    throw new ArgumentParseException(Texts.of("Handler is already in your state buffer!"), args[1], 1);
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedHandlers.add(handler);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added Handler to your state buffer!"));
                return CommandResult.success();
            } else if (isAlias(POSITIONS_ALIASES, args[0])) {
                int x, y, z;
                Vector3i pPos = player.getLocation().getBlockPosition();
                if (args.length == 1) {
                    x = pPos.getX();
                    y = pPos.getY();
                    z = pPos.getZ();
                } else if (args.length > 1 && args.length < 4) {
                    throw new CommandException(Texts.of("Not enough arguments!"));
                } else if (args.length == 4) {
                    try {
                        x = FGHelper.parseCoordinate(pPos.getX(), args[1]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[1] + "\"!"), e, args[1], 1);
                    }
                    try {
                        y = FGHelper.parseCoordinate(pPos.getY(), args[2]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[2] + "\"!"), e, args[2], 2);
                    }
                    try {
                        z = FGHelper.parseCoordinate(pPos.getZ(), args[3]);
                    } catch (NumberFormatException e) {
                        throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[3] + "\"!"), e, args[3], 3);
                    }
                } else {
                    throw new CommandException(Texts.of("Too many arguments!"));
                }
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions.add(new Vector3i(x, y, z));
                player.sendMessage(Texts.of(TextColors.GREEN, "Successfully added position (" + x + ", " + y + ", " + z + ") to your state buffer!"));
                return CommandResult.success();
            } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
        } else {

        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.state.add");
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
        if (source instanceof Player)
            return Texts.of("add <region [w:<worldname>] | handler> <name>");
        else return Texts.of("add <region <worldname> | handler> <name>");
    }
}
