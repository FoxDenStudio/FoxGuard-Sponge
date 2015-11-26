/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/
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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import net.gravityfox.foxguard.util.FGHelper;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/20/2015.
 * Project: foxguard
 */
public class CommandPosition implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        if (source instanceof Player) {
            Player player = (Player) source;
            String[] args = {};
            if (!arguments.isEmpty()) args = arguments.split(" +");
            int x, y, z;
            Vector3i pPos = player.getLocation().getBlockPosition();
            if (args.length == 0) {
                x = pPos.getX();
                y = pPos.getY();
                z = pPos.getZ();
            } else if (args.length > 0 && args.length < 3) {
                throw new CommandException(Texts.of("Not enough arguments!"));
            } else if (args.length == 3) {
                try {
                    x = FGHelper.parseCoordinate(pPos.getX(), args[0]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[0] + "\"!"), e, args[0], 0);
                }
                try {
                    y = FGHelper.parseCoordinate(pPos.getY(), args[1]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[1] + "\"!"), e, args[1], 1);
                }
                try {
                    z = FGHelper.parseCoordinate(pPos.getZ(), args[2]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[2] + "\"!"), e, args[2], 2);
                }
            } else {
                throw new CommandException(Texts.of("Too many arguments!"));
            }
            FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions.add(new Vector3i(x, y, z));
            player.sendMessage(Texts.of(TextColors.GREEN, "Successfully added position (" + x + ", " + y + ", " + z + ") to your state buffer!"));
            return CommandResult.success();
        } else  {

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
            return Texts.of("position [<x> <y> <z>]");
        else return Texts.of("position <x> <y> <z>");

    }

}
