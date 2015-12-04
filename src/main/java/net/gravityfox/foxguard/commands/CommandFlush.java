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

package net.gravityfox.foxguard.commands;

import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

import java.util.List;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.*;

public class CommandFlush implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args;
        if (arguments.isEmpty()) {
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).flush();
        } else {
            args = arguments.split(" +");
            for (String arg : args) {
                InternalCommandState.StateField type = getType(arg);
                if (type == null) throw new CommandException(Texts.of("\"" + arg + "\" is not a valid type!"));
                FGCommandMainDispatcher.getInstance().getStateMap().get(source).flush(type);
            }
        }
        source.sendMessage(Texts.of(TextColors.GREEN, "Successfully flushed!"));
        return CommandResult.empty();
    }

    public InternalCommandState.StateField getType(String input) {
        if (contains(REGIONS_ALIASES, input)) return InternalCommandState.StateField.REGIONS;
        else if (contains(HANDLERS_ALIASES, input)) return InternalCommandState.StateField.HANDLERS;
        else if (contains(POSITIONS_ALIASES, input)) return InternalCommandState.StateField.POSITIONS;
        else return null;
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.state.flush");
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
        return Texts.of("flush [regions] [handlers] [positions]");
    }
}
