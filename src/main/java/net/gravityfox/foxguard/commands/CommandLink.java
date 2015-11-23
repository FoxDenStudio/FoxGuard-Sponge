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

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.GlobalFlagSet;
import net.gravityfox.foxguard.util.FGHelper;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandLink implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +", 2);
        if (args.length == 0) {
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.size() == 0 &&
                    FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions in your state buffer!"));
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.size() == 0)
                throw new CommandException(Texts.of("You don't have any FlagSets in your state buffer!"));
            int[] successes = {0};
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.stream().forEach(
                    region -> FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.stream()
                            .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                            .forEach(flagSet -> successes[0] += FGManager.getInstance().link(region, flagSet) ? 1 : 0));
            source.sendMessage(Texts.of(TextColors.GREEN, "Successfully linked " + successes[0] + "!"));
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).flush(InternalCommandState.StateField.REGIONS, InternalCommandState.StateField.FLAGSETS);
            return CommandResult.builder().successCount(successes[0]).build();
        } else {
            if (source instanceof Player) {
                Player player = (Player) source;
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[0], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" +", 3);
                } else world = player.getWorld();
                if (args.length < 1 + flag) throw new CommandException(Texts.of("Must specify items to link!"));
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a flagset!"));
                boolean success = FGManager.getInstance().link(world, args[flag], args[1 + flag]);
                if (success) {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully linked!"));
                    return CommandResult.success();
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "There was an error linking. Check their names and alsoke sure they haven't already been linked."));
                }
            } else {

            }
        }

        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.link.add");
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
            return Texts.of("link [ [w:<worldname>] <region name> <flagset name> ]");
        else {
            return Texts.of("link [ <worldname> <region name> <flagset name> ]");
        }
    }
}
