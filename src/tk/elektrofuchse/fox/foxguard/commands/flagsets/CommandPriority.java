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

package tk.elektrofuchse.fox.foxguard.commands.flagsets;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.commands.FGCommandMainDispatcher;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/29/2015.
 * Project: foxguard
 */
public class CommandPriority implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ");
        if (args.length == 0) {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else {
            int successes = 0;
            int failures = 0;
            List<IFlagSet> flagSets = new LinkedList<>();
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.stream().forEach(flagSets::add);
            for (String flagSetName: Arrays.copyOfRange(args, 1, args.length)){
                IFlagSet flagSet = FoxGuardManager.getInstance().getFlagSet(flagSetName);
                if (flagSet != null) {
                    flagSets.add(flagSet);
                } else {
                    failures++;
                }
            }
            if (args[0].startsWith("~")) {
                int deltaPriority;
                try {
                    deltaPriority = Integer.parseInt(args[0].substring(1));
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Not a valid priority!"), e, args[0], 1);
                }
                for (IFlagSet flagSet: flagSets){
                    flagSet.setPriority(flagSet.getPriority() + deltaPriority);
                    successes++;
                }

            } else {
                int priority;
                try {
                    priority = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Not a valid priority!"), e, args[0], 0);
                }
                for (IFlagSet flagSet : flagSets) {
                    flagSet.setPriority(priority);
                    successes++;
                }
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
        return false;
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
        return Texts.of("priority <priority> [flagsets...]");
    }

}
