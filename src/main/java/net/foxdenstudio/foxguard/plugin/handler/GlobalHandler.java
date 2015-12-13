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

package net.foxdenstudio.foxguard.plugin.handler;

import net.foxdenstudio.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.foxcore.plugin.command.util.SourceState;
import net.foxdenstudio.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.foxcore.plugin.util.FCHelper;
import net.foxdenstudio.foxguard.plugin.handler.util.Flag;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;

import static net.foxdenstudio.foxcore.plugin.util.Aliases.*;

public class GlobalHandler extends HandlerBase {

    public static final String NAME = "_global";

    private Map<Flag, Tristate> map;

    public GlobalHandler() {
        super(NAME, Integer.MIN_VALUE);
        map = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Integer.MIN_VALUE;
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
    public Tristate handle(User user, Flag flag, Event event) {
        try {
            this.lock.readLock().lock();
            Flag temp = flag;
            while (temp != null && !map.containsKey(temp)) {
                temp = temp.getParent();
            }
            if (temp != null) return map.get(temp);
            else return map.get(flag);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public ProcessResult modify(String arguments, SourceState state, CommandSource source) throws CommandException {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player) return ProcessResult.failure();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).build();
        String[] args = parse.getArgs();
        try {
            this.lock.writeLock().lock();
            if (args.length > 0) {
                if (isAlias(SET_ALIASES, args[0])) {
                    if (args.length > 1) {
                        Flag flag;
                        if (args[1].equalsIgnoreCase("all")) {
                            flag = null;
                        } else {
                            flag = Flag.flagFrom(args[1]);
                            if (flag == null) {
                                return ProcessResult.of(false, Texts.of("Not a valid flag!"));
                            }
                        }
                        if (isAlias(CLEAR_ALIASES, args[2])) {
                            if (flag == null) {
                                this.map.clear();
                                return ProcessResult.of(true, Texts.of("Successfully cleared flags!"));
                            } else {
                                this.map.remove(flag);
                                return ProcessResult.of(true, Texts.of("Successfully cleared flag!"));
                            }
                        } else {
                            Tristate tristate = tristateFrom(args[2]);
                            if (tristate == null) {
                                return ProcessResult.of(false, Texts.of("Not a valid value!"));
                            }
                            if (flag == null) {
                                for (Flag thatExist : Flag.values()) {
                                    this.map.put(thatExist, tristate);
                                }
                                return ProcessResult.of(true, Texts.of("Successfully set flags!"));
                            } else {
                                this.map.put(flag, tristate);
                                return ProcessResult.of(true, Texts.of("Successfully set flag!"));
                            }

                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify a flag!"));
                    }
                } else {
                    return ProcessResult.of(false, Texts.of("Not a valid GlobalHandler command!"));
                }
            } else {
                return ProcessResult.of(false, Texts.of("Must specify a command!"));
            }
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard modify handler " + NAME + " set "),
                TextActions.showText(Texts.of("Click to Set a Flag")),
                "Global Flags:\n"));
        try {
            this.lock.readLock().lock();
            for (Flag f : this.map.keySet()) {
                builder.append(
                        Texts.builder().append(Texts.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(map.get(f)))
                                .append(Texts.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + NAME + " set " + f.flagName() + " "))
                                .onHover(TextActions.showText(Texts.of("Click to Change This Flag")))
                                .build()
                );
            }
        } finally {
            this.lock.readLock().unlock();
        }
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        this.lock.readLock().lock();
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
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void loadFromDatabase(DataSource dataSource) throws SQLException {
        this.lock.writeLock().lock();
        try (Connection conn = dataSource.getConnection()) {
            CallbackHashMap<Flag, Tristate> flagMap = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
            try (Statement statement = conn.createStatement()) {
                try (ResultSet mapEntrySet = statement.executeQuery("SELECT * FROM FLAGMAP")) {
                    while (mapEntrySet.next()) {
                        try {
                            flagMap.put(Flag.valueOf(mapEntrySet.getString("KEY")),
                                    Tristate.valueOf(mapEntrySet.getString("VALUE")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            this.map = flagMap;
        } finally {
            this.lock.writeLock().unlock();
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
