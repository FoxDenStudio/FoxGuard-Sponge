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

package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FGManager;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandModify implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ", 3);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (FGHelper.contains(regionsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" ", 4);
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FGManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new CommandException(Texts.of("No region with name \"" + args[1 + flag] + "\"!"));
                region.modify(args.length < 3 + flag ? "" : args[2 + flag],
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player), player);
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully modified!"));
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 1) throw new CommandException(Texts.of("Must specify a name!"));
                IFlagSet flagSet = FGManager.getInstance().getFlagSet(args[1]);
                if (flagSet == null)
                    throw new CommandException(Texts.of("No region with name \"" + args[1] + "\"!"));
                boolean success = flagSet.modify(args.length < 3 ? "" : args[2],
                        FGCommandMainDispatcher.getInstance().getStateMap().get(player), player);
                if (success) source.sendMessage(Texts.of(TextColors.GREEN, "Successfully modified!"));
                else source.sendMessage(Texts.of(TextColors.RED, "Could not modify!"));
            } else {
                throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
            }
        } else if (source instanceof ConsoleSource) {

        }

        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return false;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return null;
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return null;
    }

    @Override
    public Text getUsage(CommandSource source) {
        if (source instanceof Player)
            return Texts.of("detail (region [w:<worldname>] | flagset) <name> [args...]");
        else return Texts.of("detail (region <worldname> | flagset) <name> [args...]");

    }
}
