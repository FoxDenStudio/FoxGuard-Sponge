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
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class GlobalHandler extends HandlerBase implements IGlobal {

    public static final String NAME = "_global";

    private final Map<IFlag, Tristate> map;
    private final CacheMap<IFlag, Tristate> mapCache;

    public GlobalHandler() {
        super(NAME, Integer.MIN_VALUE / 2);
        this.map = new CacheMap<>((key, map) -> Tristate.UNDEFINED);
        this.mapCache = new CacheMap<>((o, m) -> {
            if (o instanceof Flag) {
                Tristate state = map.get(FGUtil.nearestParent((Flag) o, map.keySet()));
                m.put((Flag) o, state);
                return state;
            } else return Tristate.UNDEFINED;
        });
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Integer.MIN_VALUE / 2;
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public String getShortTypeName() {
        return "Global";
    }

    @Override
    public String getLongTypeName() {
        return "Global";
    }

    @Override
    public String getUniqueTypeString() {
        return "global";
    }

    @Override
    public EventResult handle(User user, IFlag flag, Optional<Event> event, Object... extra) {
        return EventResult.of(mapCache.get(flag));
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player) return ProcessResult.failure();
        }
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
                    if (parse.args.length > 2) {
                        if (isIn(CLEAR_ALIASES, parse.args[2])) {
                            if (flag == null) {
                                this.map.clear();
                                this.mapCache.clear();
                                return ProcessResult.of(true, Text.of("Successfully cleared " +
                                        "flags!"));
                            } else {
                                this.map.remove(flag);
                                this.mapCache.clear();
                                return ProcessResult.of(true, Text.of("Successfully cleared " +
                                        "flag!"));
                            }
                        } else {
                            Tristate tristate = tristateFrom(parse.args[2]);
                            if (tristate == null) {
                                return ProcessResult.of(false, Text.of("Not a valid value!"));
                            }
                            if (flag == null) {
                                for (IFlag thatExist : Flag.getFlags()) {
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
                        return ProcessResult.of(false, Text.of("Must specify a value!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a flag!"));
                }
            } else {
                return ProcessResult.of(false, Text.of("Not a valid GlobalHandler command!"));
            }
        } else {
            return ProcessResult.of(false, Text.of("Must specify a command!"));
        }

    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws
            CommandException {
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
                return Flag.getFlags().stream()
                        .map(IFlag::flagName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 2) {
                return ImmutableList.of("true", "false", "passthrough", "clear").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + NAME + " set "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Global Flags:\n"));

        for (IFlag f : this.map.keySet().stream().sorted().collect(GuavaCollectors.toImmutableList())) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FGUtil.readableTristateText(map.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard md h " + NAME + " set " +
                                    "" + f.flagName() + " "))
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

    public void load(Path directory) {
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
            Map<String, String> storageFlagMap = flagMapDB.hashMap("flags", Serializer.STRING, Serializer.STRING).createOrOpen();
            map.clear();
            for (Map.Entry<String, String> entry : storageFlagMap.entrySet()) {
                map.put(Flag.flagFrom(entry.getKey()), Tristate.valueOf(entry.getValue()));
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = true;
    }

}
