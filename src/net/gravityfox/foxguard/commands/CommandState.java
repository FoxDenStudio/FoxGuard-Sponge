/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.source.ConsoleSource;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/20/2015.
 * Project: foxguard
 */
public class CommandState implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        TextBuilder output = Texts.builder().append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"));
        if (source instanceof Player) {
            Player player = (Player) source;
            int flag = 0;
            if (!FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.isEmpty()) {
                output.append(Texts.of(TextColors.GREEN, "Regions: "));
                Iterator<IRegion> regionIterator = FGCommandMainDispatcher.getInstance().getStateMap()
                        .get(player).selectedRegions.iterator();
                int index = 1;
                while (regionIterator.hasNext()) {
                    IRegion region = regionIterator.next();
                    output.append(Texts.of(FGHelper.getColorForRegion(region),
                            "\n  " + (index++) + ": " + region.getType() + " : " + region.getWorld().getName() + " : " + region.getName()));
                }
                output.append(Texts.of("\n"));
                flag++;
            }
            if (!FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.isEmpty()) {
                if (flag != 0) output.append(Texts.of("\n"));
                output.append(Texts.of(TextColors.GREEN, "FlagSets: "));
                Iterator<IFlagSet> flagSetIterator = FGCommandMainDispatcher.getInstance().getStateMap()
                        .get(player).selectedFlagSets.iterator();
                int index = 1;
                while (flagSetIterator.hasNext()) {
                    IFlagSet flagSet = flagSetIterator.next();
                    output.append(Texts.of(FGHelper.getColorForFlagSet(flagSet),
                            "\n  " + (index++) + ": " + flagSet.getType() + " : " + flagSet.getName()));
                }
                output.append(Texts.of("\n"));
                flag++;
            }
            if (!FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions.isEmpty()) {
                if (flag != 0) output.append(Texts.of("\n"));
                output.append(Texts.of(TextColors.GREEN, "Positions:"));
                output.append(Texts.of(TextColors.RESET));
                int index = 1;
                for (Vector3i pos : FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions) {
                    output.append(
                            Texts.of("\n  " + (index++) + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                    );
                }
                flag++;
            }
            if (flag == 0) player.sendMessage(Texts.of("Your current state buffer is clear!"));
            else player.sendMessage(output.build());


        } else if (source instanceof ConsoleSource) {

        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.state");
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
        return Texts.of("state");
    }
}
