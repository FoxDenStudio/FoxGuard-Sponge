/*
 * This file is part of FoxCore, licensed under the MIT License (MIT).
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


import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CommandTest implements CommandCallable {

    /*
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this
            command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(3)
        .parse2();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "-----------------------------\n"));
        builder.append(Text.of(TextColors.GOLD, "Args: \"", TextColors.RESET, arguments,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Type: ", TextColors.RESET, parse.currentElement
        .type, TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Token: \"", TextColors.RESET, parse
        .currentElement.token, TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Index: ", TextColors.RESET, parse.currentElement
        .index, TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Key: \"", TextColors.RESET, parse.currentElement
        .key, TextColors.GOLD, "\"\n"));
        source.sendMessage(builder.build());
        return CommandResult.empty();
    }
    */

    /*@Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws
    CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .autoCloseQuotes(true)
                .limit(2)
                .parse();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "\n-----------------------------\n"));
        builder.append(Text.of(TextColors.GOLD, "Args: \"", TextColors.RESET, arguments,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Type: ", TextColors.RESET, parse.current.type,
        TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Token: \"", TextColors.RESET, parse.current
        .token, TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Index: ", TextColors.RESET, parse.current.index,
         TextColors.GOLD, "\n"));
        builder.append(Text.of(TextColors.GOLD, "Key: \"", TextColors.RESET, parse.current.key,
        TextColors.GOLD, "\"\n"));
        builder.append(Text.of(TextColors.GOLD, "Prefix: \"", TextColors.RESET, parse.current
        .prefix, TextColors.GOLD, "\"\n"));
        source.sendMessage(builder.build());
        return ImmutableList.of();
    }*/

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxcore.command.debug.test");
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
        return Text.of("test [mystery args]...");
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this " +
                    "command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------\n"));
            IFlag flag = Flag.flagFrom(parse.args[0]);
            if (flag != null) {
                for (Set<IFlag> level : flag.getHierarchy()) {
                    for (IFlag f : level) {
                        builder.append(Text.of(f.flagName() + " "));
                    }
                    builder.append(Text.of("\n"));
                }
                source.sendMessage(builder.build());
            }
        }

        return CommandResult.empty();
    }


    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws
            CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        return Arrays.stream(Flag.values())
                .map(Flag::flagName)
                .filter(new StartsWithPredicate(parse.current.token))
                .map(args -> parse.current.prefix + args)
                .collect(GuavaCollectors.toImmutableList());
    }

    /*@Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this
            command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(2)
        .parse();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "\n-----------------------------\n"));
        int count = 0;
        for (String str : parse.args) {
            count++;
            builder.append(Text.of(count + ": " + str + "\n"));
        }
        for (Map.Entry<String, String> entry : parse.flagmap.entrySet()) {
            builder.append(Text.of(entry.getKey() + " : " + entry.getValue() + "\n"));
        }
        source.sendMessage(builder.build());
        return CommandResult.empty();
    }*/
}
