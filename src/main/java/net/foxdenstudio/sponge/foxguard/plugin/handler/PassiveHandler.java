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
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
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
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class PassiveHandler extends HandlerBase {

    private final Map<Flag, Tristate> map;

    public PassiveHandler(String name, int priority) {
        this(name, priority, new CacheMap<>((key, map) -> Tristate.UNDEFINED));
    }

    public PassiveHandler(String name, int priority, CacheMap<Flag, Tristate> map) {
        super(name, priority);
        this.map = map;
    }

    @Override
    public EventResult handle(@Nullable User user, Flag flag, Event event) {
        if (user != null) {
            return EventResult.pass();
        }
        return EventResult.of(map.get(FGUtil.nearestParent(flag, map.keySet())));
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            if (isIn(SET_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    Flag flag;
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
                            return ProcessResult.of(true, Text.of("Successfully cleared flags!"));
                        } else {
                            this.map.remove(flag);
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
                            return ProcessResult.of(true, Text.of("Successfully set flags!"));
                        } else {
                            this.map.put(flag, tristate);
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
        for (Flag f : this.map.keySet().stream().sorted().collect(GuavaCollectors.toImmutableList())) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCHelper.readableTristateText(map.get(f)))
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
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS FLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM FLAGMAP;");
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO FLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : map.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
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
        public IHandler create(DataSource source, String name, int priority, boolean isEnabled) throws SQLException {
            List<User> ownerList = new ArrayList<>();
            CacheMap<Flag, Tristate> flagMap = new CacheMap<>((key, map) -> Tristate.UNDEFINED);
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent() && !FCHelper.isUserOnList(ownerList, user.get()))
                                ownerList.add(user.get());
                        }
                    }
                    try (ResultSet passiveMapEntrySet = statement.executeQuery("SELECT * FROM FLAGMAP")) {
                        while (passiveMapEntrySet.next()) {
                            try {
                                flagMap.put(Flag.valueOf(passiveMapEntrySet.getString("KEY")),
                                        Tristate.valueOf(passiveMapEntrySet.getString("VALUE")));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            }
            PassiveHandler handler = new PassiveHandler(name, priority, flagMap);
            handler.setIsEnabled(isEnabled);
            return handler;
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
