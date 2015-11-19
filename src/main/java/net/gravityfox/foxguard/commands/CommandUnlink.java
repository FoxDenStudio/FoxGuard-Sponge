/*
 *
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

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.GlobalFlagSet;
import net.gravityfox.foxguard.flagsets.IFlagSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandUnlink implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ", 2);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0 &&
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions in your state buffer!"));
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any FlagSets in your state buffer!"));
                int[] count = {0};
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.stream().forEach(
                        region -> FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.stream()
                                .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                .forEach(flagSet -> count[0] += FGManager.getInstance().unlink(region, flagSet) ? 1 : 0));
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).flush(InternalCommandState.StateField.REGIONS, InternalCommandState.StateField.FLAGSETS);
                return CommandResult.builder().successCount(count[0]).build();
            } else if (args[0].equals("FULL")) {
                if (FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0 &&
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
                int[] count = {0};
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.stream().forEach(
                        region -> {
                            List<IFlagSet> flagSets = new ArrayList<>();
                            region.getFlagSets().stream()
                                    .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                    .forEach(flagSets::add);
                            flagSets.stream().forEach(flagSet -> count[0] += FGManager.getInstance().unlink(region, flagSet) ? 1 : 0);
                        });
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.stream()
                        .filter(flagSet -> !(flagSet instanceof GlobalFlagSet)).forEach(
                        flagSet -> FGManager.getInstance().getRegionsListCopy().stream().forEach(
                                region -> count[0] += FGManager.getInstance().unlink(region, flagSet) ? 1 : 0));
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).flush(InternalCommandState.StateField.REGIONS, InternalCommandState.StateField.FLAGSETS);
                return CommandResult.builder().successCount(count[0]).build();
            } else if (args[0].equals("ALL")) {
                int[] count = {0};
                FGManager.getInstance().getRegionsListCopy().forEach(
                        region -> {
                            List<IFlagSet> flagSets = new ArrayList<>();
                            region.getFlagSets().stream()
                                    .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                    .forEach(flagSets::add);
                            flagSets.stream().forEach(flagSet -> count[0] += FGManager.getInstance().unlink(region, flagSet) ? 1 : 0);
                        });
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                return CommandResult.builder().successCount(count[0]).build();
            }
        } else {

        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.link.remove");
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
        return Texts.of("unlink (FULL | ALL)");
    }
}
