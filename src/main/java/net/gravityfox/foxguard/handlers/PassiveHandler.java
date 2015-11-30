/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.handlers;

import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.handlers.util.Flags;
import net.gravityfox.foxguard.util.CallbackHashMap;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ProxySource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.gravityfox.foxguard.util.Aliases.*;

/**
 * Created by Fox on 11/26/2015.
 * Project: SpongeForge
 */
public class PassiveHandler extends OwnableHandlerBase {

    Flags[] availableFlags = {Flags.SPAWN_MOB_PASSIVE, Flags.SPAWN_MOB_HOSTILE};

    private Map<Flags, Tristate> passiveMap;

    public PassiveHandler(String name, int priority) {
        this(name, priority, new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED));
    }

    public PassiveHandler(String name, int priority, CallbackHashMap<Flags, Tristate> map) {
        super(name, priority);
        this.passiveMap = map;
    }

    @Override
    public Tristate handle(@Nullable User user, Flags flag, Event event) {
        try {
            this.lock.readLock().lock();
            if (!isEnabled || user != null) {
                this.lock.readLock().unlock();
                return Tristate.UNDEFINED;
            }
            if (FGHelper.contains(availableFlags, flag)) {
                this.lock.readLock().unlock();
                return passiveMap.get(flag);
            }
        } finally {
            this.lock.readLock().unlock();
        }
        return Tristate.UNDEFINED;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return false;
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        try {
            this.lock.writeLock().lock();
            if (args.length > 0) {
                if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                    if (args.length > 1) {
                        UserOperations op;
                        if (args[2].equalsIgnoreCase("add")) {
                            op = UserOperations.ADD;
                        } else if (args[2].equalsIgnoreCase("remove")) {
                            op = UserOperations.REMOVE;
                        } else if (args[2].equalsIgnoreCase("set")) {
                            op = UserOperations.SET;
                        } else {
                            source.sendMessage(Texts.of(TextColors.RED, "Not a valid operation!"));
                            return false;
                        }
                        if (args.length > 2) {
                            int successes = 0;
                            int failures = 0;
                            List<String> names = new ArrayList<>();
                            for (String name : Arrays.copyOfRange(args, 3, args.length)) {
                                names.add(name);
                            }
                            List<User> users = new ArrayList<>();
                            for (String name : names) {
                                Optional<User> optUser = FoxGuardMain.getInstance().getUserStorage().get(name);
                                if (optUser.isPresent() && !FGHelper.isUserOnList(users, optUser.get()))
                                    users.add(optUser.get());
                                else failures++;
                            }
                            switch (op) {
                                case ADD:
                                    for (User user : users) {
                                        if (!FGHelper.isUserOnList(ownerList, user) && ownerList.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User cUser : ownerList) {
                                        if (FGHelper.isUserOnList(users, cUser)) {
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
                            source.sendMessage(Texts.of(TextColors.GREEN, "Modified list with " + successes + " successes and " + failures + " failures."));
                            return true;
                        } else {
                            source.sendMessage(Texts.of(TextColors.RED, "Must specify one or more users!"));
                            return false;
                        }
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Must specify an operation!"));
                        return false;
                    }

                } else if (isAlias(SET_ALIASES, args[0])) {
                    if (args.length > 1) {
                        Flags flag = Flags.flagFrom(args[1]);
                        if (flag == null) {
                            source.sendMessage(Texts.of(TextColors.RED, "Not a valid flag!"));
                            return false;
                        }
                        if (args.length > 2) {
                            Tristate tristate = tristateFrom(args[2]);
                            if (tristate == null) {
                                source.sendMessage(Texts.of(TextColors.RED, "Not a valid value!"));
                                return false;
                            }
                            passiveMap.put(flag, tristate);
                            source.sendMessage(Texts.of(TextColors.GREEN, "Successfully set flag!"));
                            return true;
                        } else {
                            source.sendMessage(Texts.of(TextColors.RED, "Must specify a value!"));
                            return false;
                        }
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Must specify a flag!"));
                        return false;
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Not a valid PassiveHandler command!"));
                    return false;
                }
            } else {
                source.sendMessage(Texts.of(TextColors.RED, "Must specify a command!"));
                return false;
            }
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = super.getDetails(arguments).builder();
        builder.append(Texts.of("\n"));
        builder.append(Texts.of(TextColors.GOLD, "Passive Flags:"));
        try {
            this.lock.readLock().lock();
            for (Flags f : this.passiveMap.keySet()) {
                builder.append(Texts.of("\n  " + f.toString() + ": " + FGHelper.readableTristate(this.passiveMap.get(f))));
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
                for (Map.Entry<Flags, Tristate> entry : passiveMap.entrySet()) {
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
