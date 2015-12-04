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

import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandException;

import java.util.ArrayList;
import java.util.HashMap;
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

    private String[] args = {};
    private Map<String, String> flagmap = new HashMap<>();

    private AdvCmdParse(String arguments, int limit, boolean subFlags,
                        Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper) throws CommandException {
        {
            Pattern pattern = Pattern.compile("\"");
            Matcher matcher = pattern.matcher(arguments);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count % 2 == 1) {
                throw new CommandException(Texts.of("You must close all quotes!"));
            }
        }
        String toParse = arguments.trim();
        List<String> argsList = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\S*\".+?\")|(\\S+)");
        Matcher matcher = pattern.matcher(toParse);
        while (matcher.find()) {
            String result = matcher.group();
            if (!result.startsWith("---")) {
                if (result.startsWith("--") && !(subFlags && limit != 0 && argsList.size() > limit)) {
                    result = result.substring(2);
                    String[] parts = result.split("[:=]", 2);
                    if (parts[0].contains("\""))
                        throw new CommandException(Texts.of("You may not have quotes in flag keys!"));
                    String value = "";
                    if(parts.length > 1) value = trimQuotes(parts[1]);
                    flagMapper.apply(this.flagmap).apply(parts[0]).accept(value);
                } else if (result.startsWith("-") && !(subFlags && limit != 0 && argsList.size() > limit)) {
                    result = result.substring(1);
                    for (String str : result.split("")) {
                        if (str.matches("[a-zA-Z0-9]")) {
                            String temp = str;
                            while (this.flagmap.containsKey(temp)) {
                                temp += str;
                            }
                            this.flagmap.put(temp, "");
                        } else {
                            throw new CommandException(Texts.of("You may only have alphanumeric short keys!"));
                        }
                    }
                } else {
                    argsList.add(trimQuotes(result));
                }
            }
        }
        List<String> finalList = new ArrayList<>();
        String finalString = "";
        for (int i = 0; i < argsList.size(); i++) {
            if (limit == 0 || i < limit) {
                finalList.add(argsList.get(i));
            } else {
                finalString += argsList.get(i);
                if (i + 1 < argsList.size()) {
                    finalString += " ";
                }
            }
        }
        if(limit != 0) {
            finalList.add(finalString);
        }
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

    private String trimQuotes(String str) {
        if (str.startsWith("\"")) str = str.substring(1);
        if (str.endsWith("\"")) str = str.substring(0, str.length() - 1);
        return str;
    }

    public static class AdvCmdParseBuilder {

        private String arguments = "";
        private int limit = 0;
        private boolean subFlags = false;
        private Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper = DEFAULT_MAPPER;

        private AdvCmdParseBuilder() {
        }

        public AdvCmdParseBuilder setArguments(String arguments) {
            this.arguments = arguments;
            return this;
        }

        public AdvCmdParseBuilder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public AdvCmdParseBuilder setSubFlags(boolean subFlags) {
            this.subFlags = subFlags;
            return this;
        }

        public AdvCmdParseBuilder setFlagMapper(Function<Map<String, String>, Function<String, Consumer<String>>> flagMapper) {
            this.flagMapper = flagMapper;
            return this;
        }

        public AdvCmdParse build() throws CommandException {
            return new AdvCmdParse(arguments, limit, subFlags, flagMapper);
        }
    }
}
