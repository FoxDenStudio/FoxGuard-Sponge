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

package net.gravityfox.foxguard.commands.util;

import net.gravityfox.foxguard.util.CallbackHashMap;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.command.CommandException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Fox on 12/1/2015.
 * Project: SpongeForge
 */
public class AdvCmdParse {

    public static final Function<Map<String, String>, Function<String, Consumer<String>>>
            DEFAULT_MAPPER = map -> key -> value -> map.put(key, value);
    private static final String regex = "(?:(?:\\\\.|[^\"'\\s])*[:=-])?([\"'])(?:\\\\.|[^\\\\])*?\\1|(?:\\\\.|[^\"'\\s])+";

    private String[] args = {};
    private Map<String, String> flagmap = new CallbackHashMap<>((key, map) -> "");

    private AdvCmdParse(String arguments, int limit, boolean extractSubFlags, boolean unescapeLast, boolean autoCloseQuotes,
                        Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper) throws CommandException {
        // Regex Pattern for identifying arguments and flags. It respects quotation marks and escape characters.
        Pattern pattern = Pattern.compile(regex);
        // Check for unclosed quotes
        {
            String toStrip = arguments;
            while (true) {
                Matcher tempMatcher = pattern.matcher(toStrip);
                if (!tempMatcher.find()) break;
                toStrip = toStrip.substring(0, tempMatcher.start()) + toStrip.substring(tempMatcher.end());
            }
            Pattern pattern2 = Pattern.compile("[\"']");
            Matcher matcher = pattern2.matcher(toStrip);
            if (matcher.find()) {
                if (autoCloseQuotes) {
                    if (matcher.group().equals("\"")) arguments += "\"";
                    if (matcher.group().equals("'")) arguments += "'";
                } else {
                    throw new CommandException(Texts.of("You must close all quotes!"));
                }
            }
        }

        // String to parse
        // List of string arguments that were not parsed as flags
        List<String> argsList = new ArrayList<>();
        // Matcher for identifying arguments and flags.
        Matcher matcher = pattern.matcher(arguments);
        // Iterate through matches
        while (matcher.find())

        {
            String result = matcher.group();
            // Makes "---" mark the end of the command. Effectively allows command comments
            // It also means that flag names cannot start with hyphens
            if (!result.startsWith("---")) {
                // Throws out any results that are empty
                if (result.equals("--") || result.equals("-")) continue;
                // Parses result as long flag.
                // Format is --<flagname>:<value> Where value can be a quoted string. "=" is also a valid separator
                // If a limit is specified, the flags will be cut out of the final string
                // Setting extractSubFlags to true forces flags within the final string to be left as-is
                // This is useful if the final string is it's own command and needs to be re-parsed
                if (result.startsWith("--") && !(!extractSubFlags && limit != 0 && argsList.size() > limit)) {
                    // Trims the prefix
                    result = result.substring(2);
                    // Splits once by ":" or "="
                    String[] parts = result.split("[:=]", 2);
                    // Throw an exception if the key contains a quote character, as that shouldn't be allowed
                    if (parts[0].contains("\"") || parts[0].contains("'"))
                        throw new CommandException(Texts.of("You may not have quotes in long flag keys!"));
                    // Default value in case a value isn't specified
                    String value = "";
                    // Retrieves value if it exists
                    if (parts.length > 1) value = unescapeString(parts[1]);
                    // Applies the flagMapper function.
                    // This is a destructive function that takes 3 parameters and returns nothing. (Destructive consumer)
                    flagMapper.apply(this.flagmap)
                            .apply(parts[0])
                            .accept(value);

                    // Parses result as a short flag. Limit behavior is the same as long flags
                    // multiple letters are treated as multiple flags. Repeating letters add a second flag with a repetition
                    // Example: "-aab" becomes flags "a", "aa", and "b"
                } else if (result.startsWith("-") && result.substring(1, 2).matches("[\\D]") && !(!extractSubFlags && limit != 0 && argsList.size() > limit)) {
                    // Trims prefix
                    result = result.substring(1);
                    // Iterates through each letter
                    for (String str : result.split("")) {
                        // Checks to make sure that the flag letter is alphabetic. Throw exception if it doesn't
                        if (str.matches("[a-zA-Z]")) {
                            // Checks if the flag already exists, and repeat the letter until it doesn't
                            String temp = str;
                            while (this.flagmap.containsKey(temp)) {
                                temp += str;
                            }
                            // Applies destructive flagMapper function.
                            flagMapper.apply(this.flagmap).apply(temp).accept("");
                        } else if (str.matches("[:=-]")) {
                            throw new CommandException(Texts.of("You may only have alphanumeric short flags! Did you mean to use a long flag (two dashes)?"));
                        } else {
                            throw new CommandException(Texts.of("You may only have alphanumeric short flags!"));
                        }
                    }

                    // Simply adds the result to the argument list. Quotes are trimmed.
                    // Fallback if the result isn't a flag.
                } else {
                    if (limit != 0 && argsList.size() >= limit && !unescapeLast) argsList.add(result);
                    else argsList.add(unescapeString(result));
                }
            } else break;
        }

        // This part converts the argument list to the final argument array.
        // A number of arguments are copied to a new list less than or equal to the limit.
        // The rest of the arguments, if any, are concatenated together.
        List<String> finalList = new ArrayList<>();
        String finalString = "";
        for (
                int i = 0;
                i < argsList.size(); i++)

        {
            if (limit == 0 || i < limit) {
                finalList.add(argsList.get(i));
            } else {
                finalString += argsList.get(i);
                if (i + 1 < argsList.size()) {
                    finalString += " ";
                }
            }
        }

        if (!finalString.isEmpty())

        {
            finalList.add(finalString);
        }
        // Converts final argument list to an array.
        args = finalList.toArray(new String[finalList.size()]);
    }

    public static AdvCmdParseBuilder builder() {
        return new AdvCmdParseBuilder();
    }

    public String[] getArgs() {
        return args;
    }

    public Map<String, String> getFlagmap() {
        return flagmap;
    }

    private String unescapeString(String str) {
        if (str.startsWith("\"") || str.startsWith("'")) str = str.substring(1, str.length() - 1);
        String newStr = "";
        for (int i = 0; i < str.length(); i++) {
            String letter = str.substring(i, i + 1);
            if (letter.equals("\\")) {
                if (i + 2 <= str.length()) {
                    String escape = str.substring(i + 1, i + 2);
                    switch (escape) {
                        case "n":
                            escape = "\n";
                    }
                    newStr += escape;
                    i++;
                }
            } else {
                newStr += letter;
            }
        }
        return newStr;
    }

    public static class AdvCmdParseBuilder {

        private String arguments = "";
        private int limit = 0;
        private boolean extractSubFlags = false;
        private Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper = DEFAULT_MAPPER;
        private boolean unescapeLast = false;
        private boolean autoCloseQuotes = false;

        private AdvCmdParseBuilder() {
        }

        public AdvCmdParseBuilder arguments(String arguments) {
            this.arguments = arguments;
            return this;
        }

        public AdvCmdParseBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public AdvCmdParseBuilder extractSubFlags(boolean subFlags) {
            this.extractSubFlags = subFlags;
            return this;
        }

        public AdvCmdParseBuilder unescapeLast(boolean unescapeLast) {
            this.unescapeLast = unescapeLast;
            return this;
        }

        public AdvCmdParseBuilder autoCloseQuotes(boolean autoCloseQuotes) {
            this.autoCloseQuotes = autoCloseQuotes;
            return this;
        }

        public AdvCmdParseBuilder flagMapper(Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper) {
            this.flagMapper = flagMapper;
            return this;
        }

        public AdvCmdParse build() throws CommandException {
            return new AdvCmdParse(arguments, limit, extractSubFlags, unescapeLast, autoCloseQuotes, flagMapper);
        }
    }
}
