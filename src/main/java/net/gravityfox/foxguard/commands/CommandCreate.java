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
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.factory.FGFactoryManager;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class CommandCreate implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    //create region [w:<world>] name type args...
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +", 4);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
                //----------------------------------------------------------------------------------------------------------------------
            } else if (FGHelper.contains(regionsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" +", 5);
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                if (args[1 + flag].matches("^.*[^0-9a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1 + flag], 1 + flag);
                if (args[1 + flag].matches("^[^a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1 + flag], 1 + flag);
                if (args[1 + flag].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state"))
                    throw new CommandException(Texts.of("You may not use \"" + args[1 + flag] + "\" as a name!"));
                if (args.length < 3 + flag) throw new CommandException(Texts.of("Must specify a type!"));
                IRegion newRegion = FGFactoryManager.getInstance().createRegion(
                        args[1 + flag].toLowerCase(), args[2 + flag],
                        args.length < 4 + flag ? "" : args[3 + flag],
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player), world, player);
                if (newRegion == null)
                    throw new CommandException(Texts.of("Failed to create Region! Perhaps the type is invalid?"));
                boolean success = FGManager.getInstance().addRegion(world, newRegion);
                if (!success)
                    throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1 + flag], 1 + flag);
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).flush(InternalCommandState.StateField.POSITIONS);
                player.sendMessage(Texts.of(TextColors.GREEN, "Region created successfully"));
                return CommandResult.success();
                //----------------------------------------------------------------------------------------------------------------------
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                if (args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1], 1);
                if (args[1].matches("^[^a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1], 1);
                if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state"))
                    throw new CommandException(Texts.of("You may not use \"" + args[1] + "\" as a name!"));
                int flag = 0;
                int priority = 0;
                try {
                    priority = Integer.parseInt(args[2]);
                    flag = 1;
                    args = arguments.split(" +", 5);
                } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                }

                if (args.length < 3 + flag) throw new CommandException(Texts.of("Must specify a type!"));
                IFlagSet newFlagSet = FGFactoryManager.getInstance().createFlagSet(
                        args[1].toLowerCase(), args[2 + flag], priority,
                        args.length < 4 + flag ? "" : args[3 + flag],
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player), player);
                if (newFlagSet == null)
                    throw new CommandException(Texts.of("Failed to create FlagSet! Perhaps the type is invalid?"));
                boolean success = FGManager.getInstance().addFlagSet(newFlagSet);
                if (!success)
                    throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1], 1);
                player.sendMessage(Texts.of(TextColors.GREEN, "FlagSet created successfully!"));
                return CommandResult.success();
                //----------------------------------------------------------------------------------------------------------------------
            } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
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
        return source.hasPermission("foxguard.command.modify.objects.create");
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
            return Texts.of("create <region [w:<world>] | flagset> <name> [priority] <type> [args...]");
        else return Texts.of("create <region <world> | flagset> <name> [priority] <type> [args...]");
    }
}
