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

package net.foxdenstudio.sponge.foxguard.plugin.handler;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class PassiveHandler extends HandlerBase {

    private final CacheMap<IFlag, Tristate> map;
    private final CacheMap<IFlag, Tristate> mapCache;

    public PassiveHandler(String name, int priority) {
        this(name, priority, new CacheMap<>((key, map) -> Tristate.UNDEFINED));
    }

    public PassiveHandler(String name, int priority, CacheMap<IFlag, Tristate> map) {
        super(name, priority);
        this.map = map;
        this.mapCache = new CacheMap<>((o, m) -> {
            if (o instanceof Flag) {
                Tristate state = map.get(FGUtil.nearestParent((Flag) o, map.keySet()));
                m.put((Flag) o, state);
                return state;
            } else return Tristate.UNDEFINED;
        });
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Optional<Event> event, Object... extra) {
        if (user != null) {
            return EventResult.pass();
        }
        return EventResult.of(mapCache.get(flag));
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            if (isIn(SET_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    IFlag flag;
                    if (parse.args[1].equalsIgnoreCase("all")) {
                        flag = null;
                    } else {
                        flag = Flag.flagFrom(parse.args[1]);
                        if (flag == null) {
                            return ProcessResult.of(false, Text.of("Not a valid flag!"));
                        }
                    }
                    if (isIn(CLEAR_ALIASES, parse.args[2])) {
                        if (flag == null) {
                            this.map.clear();
                            this.mapCache.clear();
                            return ProcessResult.of(true, Text.of("Successfully cleared flags!"));
                        } else {
                            this.map.remove(flag);
                            this.mapCache.clear();
                            return ProcessResult.of(true, Text.of("Successfully cleared flag!"));
                        }
                    } else {
                        Tristate tristate = tristateFrom(parse.args[2]);
                        if (tristate == null) {
                            return ProcessResult.of(false, Text.of("Not a valid value!"));
                        }
                        if (flag == null) {
                            for (Flag thatExist : Flag.values()) {
                                this.map.put(thatExist, tristate);
                            }
                            this.mapCache.clear();
                            return ProcessResult.of(true, Text.of("Successfully set flags!"));
                        } else {
                            this.map.put(flag, tristate);
                            this.mapCache.clear();
                            return ProcessResult.of(true, Text.of("Successfully set flag!"));
                        }

                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a flag!"));
                }
            } else {
                return ProcessResult.of(false, Text.of("Not a valid PassiveHandler command!"));
            }
        } else {
            return ProcessResult.of(false, Text.of("Must specify a command!"));
        }
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("set").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(SET_ALIASES, parse.args[0])) {
                    return Arrays.stream(Flag.values())
                            .map(Flag::flagName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                if (isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "clear").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.name + " set "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Passive Flags:\n"));
        for (IFlag f : this.map.keySet().stream().sorted().collect(GuavaCollectors.toImmutableList())) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCUtil.readableTristateText(map.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " set " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
            Map<String, String> storageFlagMap = flagMapDB.hashMap("flags", Serializer.STRING, Serializer.STRING).createOrOpen();
            storageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : map.entrySet()) {
                storageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
        }
    }


    @Override
    public String getShortTypeName() {
        return "Pass";
    }

    @Override
    public String getLongTypeName() {
        return "Passive";
    }

    @Override
    public String getUniqueTypeString() {
        return "passive";
    }

    public static class Factory implements IHandlerFactory {

        private final String[] passiveAliases = {"passive", "pass"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) {
            return new PassiveHandler(name, priority);
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            try(DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
                Map<String, String> storageFlagMap = flagMapDB.hashMap("flags", Serializer.STRING, Serializer.STRING).createOrOpen();
                CacheMap<IFlag, Tristate> map = new CacheMap<>((k, m) -> Tristate.UNDEFINED);
                for (Map.Entry<String, String> entry : storageFlagMap.entrySet()) {
                    map.put(Flag.flagFrom(entry.getKey()), Tristate.valueOf(entry.getValue()));
                }
                return new PassiveHandler(name, priority, map);
            }
        }

        @Override
        public String[] getAliases() {
            return passiveAliases;
        }

        @Override
        public String getType() {
            return "passive";
        }

        @Override
        public String getPrimaryAlias() {
            return "passive";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }
}
