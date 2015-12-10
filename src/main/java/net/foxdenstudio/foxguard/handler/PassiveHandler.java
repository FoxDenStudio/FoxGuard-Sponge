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

package net.foxdenstudio.foxguard.handler;

import net.foxdenstudio.foxcore.command.util.ProcessResult;
import net.foxdenstudio.foxcore.command.util.SourceState;
import net.foxdenstudio.foxcore.util.CallbackHashMap;
import net.foxdenstudio.foxcore.util.FCHelper;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.handler.util.Flag;
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

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.foxdenstudio.foxcore.util.Aliases.*;

public class PassiveHandler extends OwnableHandlerBase {

    private Map<Flag, Tristate> passiveMap;

    public PassiveHandler(String name, int priority) {
        this(name, priority, new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED));
    }

    public PassiveHandler(String name, int priority, CallbackHashMap<Flag, Tristate> map) {
        super(name, priority);
        this.passiveMap = map;
    }

    @Override
    public Tristate handle(@Nullable User user, Flag flag, Event event) {
        try {
            this.lock.readLock().lock();
            if (!isEnabled || user != null) {
                return Tristate.UNDEFINED;
            }
            Flag temp = flag;
            while (temp != null && !passiveMap.containsKey(temp)) {
                temp = temp.getParent();
            }
            if (temp != null) return passiveMap.get(temp);
            else return passiveMap.get(flag);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public ProcessResult modify(String arguments, SourceState state, CommandSource source) {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return ProcessResult.failure();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        try {
            this.lock.writeLock().lock();
            if (args.length > 0) {
                if (isAlias(OWNER_GROUP_ALIASES, args[0])) {
                    if (args.length > 1) {
                        UserOperations op;
                        if (args[1].equalsIgnoreCase("add")) {
                            op = UserOperations.ADD;
                        } else if (args[1].equalsIgnoreCase("remove")) {
                            op = UserOperations.REMOVE;
                        } else if (args[1].equalsIgnoreCase("set")) {
                            op = UserOperations.SET;
                        } else {
                            return ProcessResult.of(false, Texts.of("Not a valid operation!"));
                        }
                        if (args.length > 2) {
                            int successes = 0;
                            int failures = 0;
                            List<String> names = new ArrayList<>();
                            for (String name : Arrays.copyOfRange(args, 2, args.length)) {
                                names.add(name);
                            }
                            List<User> users = new ArrayList<>();
                            for (String name : names) {
                                Optional<User> optUser = FoxGuardMain.instance().getUserStorage().get(name);
                                if (optUser.isPresent() && !FCHelper.isUserOnList(users, optUser.get()))
                                    users.add(optUser.get());
                                else failures++;
                            }
                            switch (op) {
                                case ADD:
                                    for (User user : users) {
                                        if (!FCHelper.isUserOnList(ownerList, user) && ownerList.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User cUser : ownerList) {
                                        if (FCHelper.isUserOnList(users, cUser)) {
                                            ownerList.remove(cUser);
                                            successes++;
                                        } else failures++;
                                    }
                                    break;
                                case SET:
                                    ownerList.clear();
                                    for (User user : users) {
                                        ownerList.add(user);
                                        successes++;
                                    }
                            }
                            return ProcessResult.of(true, Texts.of("Modified list with " + successes + " successes and " + failures + " failures."));
                        } else {
                            return ProcessResult.of(false, Texts.of("Must specify one or more users!"));
                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify an operation!"));
                    }

                } else if (isAlias(SET_ALIASES, args[0])) {
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
                                this.passiveMap.clear();
                                return ProcessResult.of(true, Texts.of("Successfully cleared flags!"));
                            } else {
                                this.passiveMap.remove(flag);
                                return ProcessResult.of(true, Texts.of("Successfully cleared flag!"));
                            }
                        } else {
                            Tristate tristate = tristateFrom(args[2]);
                            if (tristate == null) {
                                return ProcessResult.of(false, Texts.of("Not a valid value!"));
                            }
                            if (flag == null) {
                                for (Flag thatExist : Flag.values()) {
                                    this.passiveMap.put(thatExist, tristate);
                                }
                                return ProcessResult.of(true, Texts.of("Successfully set flags!"));
                            } else {
                                this.passiveMap.put(flag, tristate);
                                return ProcessResult.of(true, Texts.of("Successfully set flag!"));
                            }

                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify a flag!"));
                    }
                } else {
                    return ProcessResult.of(false, Texts.of("Not a valid PassiveHandler command!"));
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
        TextBuilder builder = super.getDetails(arguments).builder();
        builder.append(Texts.of("\n"));
        builder.append(Texts.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " set "),
                TextActions.showText(Texts.of("Click to Set a Flag")),
                "Passive Flags:"));
        try {
            this.lock.readLock().lock();
            for (Flag f : this.passiveMap.keySet()) {
                builder.append(
                        Texts.builder().append(Texts.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(passiveMap.get(f)))
                                .append(Texts.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set " + f.flagName() + " "))
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
        super.writeToDatabase(dataSource);
        this.lock.readLock().lock();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS FLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM FLAGMAP;");
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO FLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : passiveMap.entrySet()) {
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

    @Override
    protected String getAddOwnerSuggestion() {
        return "/foxguard modify handler " + this.getName() + " owners add ";
    }

    @Override
    protected String getRemoveOwnerSuggestion(User user) {
        return "/foxguard modify handler " + this.getName() + " owners remove " + user.getName();
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

}
