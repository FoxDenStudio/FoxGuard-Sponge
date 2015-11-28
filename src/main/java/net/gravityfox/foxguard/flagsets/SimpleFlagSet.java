/*
 *
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

package net.gravityfox.foxguard.flagsets;

import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.util.Flags;
import net.gravityfox.foxguard.objects.IMembership;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.gravityfox.foxguard.util.Aliases.*;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class SimpleFlagSet extends OwnableFlagSetBase implements IMembership {

    private PassiveOptions passiveOption = PassiveOptions.PASSTHROUGH;

    private List<User> memberList = new LinkedList<>();
    private Map<Flags, Tristate> ownerPermissions;
    private Map<Flags, Tristate> memberPermissions;
    private Map<Flags, Tristate> defaultPermissions;

    public SimpleFlagSet(String name, int priority) {
        this(name, priority,
                new CallbackHashMap<>((o, m) -> Tristate.TRUE),
                new CallbackHashMap<>((o, m) -> Tristate.UNDEFINED),
                new CallbackHashMap<>((o, m) -> Tristate.FALSE));
    }

    public SimpleFlagSet(String name, int priority,
                         Map<Flags, Tristate> ownerPermissions,
                         Map<Flags, Tristate> memberPermissions,
                         Map<Flags, Tristate> defaultPermissions) {
        super(name, priority);
        this.ownerPermissions = ownerPermissions;
        this.memberPermissions = memberPermissions;
        this.defaultPermissions = defaultPermissions;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.flagsets")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return false;
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (args.length > 0) {
            if (isAlias(GROUPS_ALIASES, args[0])) {
                if (args.length > 1) {
                    List<User> list;
                    if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                        list = this.ownerList;
                    } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                        list = this.memberList;
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Not a valid group!"));
                        return false;
                    }
                    if (args.length > 2) {
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
                        if (args.length > 3) {
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
                                        if (!FGHelper.isUserOnList(list, user) && list.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User cUser : list) {
                                        if (FGHelper.isUserOnList(users, cUser)) {
                                            list.remove(cUser);
                                            successes++;
                                        } else failures++;
                                    }
                                    break;
                                case SET:
                                    list.clear();
                                    for (User user : users) {
                                        list.add(user);
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
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Must specify a group!"));
                    return false;
                }
            } else if (isAlias(SET_ALIASES, args[0])) {
                Map<Flags, Tristate> map;
                if (args.length > 1) {
                    if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                        map = ownerPermissions;
                    } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                        map = memberPermissions;
                    } else if (isAlias(DEFAULT_GROUP_ALIASES, args[1])) {
                        map = defaultPermissions;
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Not a valid group!"));
                        return false;
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Must specify a group!"));
                    return false;
                }
                if (args.length > 2) {
                    Flags flag = Flags.flagFrom(args[2]);
                    if (flag == null) {
                        source.sendMessage(Texts.of(TextColors.RED, "Not a valid flag!"));
                        return false;
                    }
                    if (args.length > 3) {
                        Tristate tristate = tristateFrom(args[3]);
                        if (tristate == null) {
                            source.sendMessage(Texts.of(TextColors.RED, "Not a valid value!"));
                            return false;
                        }
                        map.put(flag, tristate);
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
            } else if (isAlias(PASSIVE_ALIASES, args[0])) {
                if (args.length > 1) {
                    if (isAlias(TRUE_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.ALLOW;
                        return true;
                    } else if (isAlias(FALSE_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.DENY;
                        return true;
                    } else if (isAlias(PASSTHROUGH_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.PASSTHROUGH;
                        return true;
                    } else if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.OWNER;
                        return true;
                    } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.MEMBER;
                        return true;
                    } else if (isAlias(DEFAULT_GROUP_ALIASES, args[1])) {
                        this.passiveOption = PassiveOptions.DEFAULT;
                        return true;
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Not a valid option!"));
                        return false;
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Must specify an option!"));
                    return false;
                }
            } else {
                source.sendMessage(Texts.of(TextColors.RED, "Not a valid SimpleFlagset command!"));
                return false;
            }
        } else {
            source.sendMessage(Texts.of(TextColors.RED, "Must specify a command!"));
            return false;
        }
    }

    @Override
    public Tristate isAllowed(User user, Flags flag, Event event) {
        if (!isEnabled) return Tristate.UNDEFINED;
        if (user == null) {
            switch (this.passiveOption) {
                case OWNER:
                    return this.ownerPermissions.get(flag);
                case MEMBER:
                    return this.memberPermissions.get(flag);
                case DEFAULT:
                    return this.defaultPermissions.get(flag);
                case ALLOW:
                    return Tristate.TRUE;
                case DENY:
                    return Tristate.FALSE;
                case PASSTHROUGH:
                    return Tristate.UNDEFINED;
            }
        }
        if (FGHelper.isUserOnList(this.ownerList, user)) return this.ownerPermissions.get(flag);
        if (FGHelper.isUserOnList(this.memberList, user)) return this.memberPermissions.get(flag);
        return this.defaultPermissions.get(flag);
    }

    @Override
    public String getShortTypeName() {
        return "Simple";
    }

    @Override
    public String getLongTypeName() {
        return "Simple";
    }

    @Override
    public String getUniqueTypeString() {
        return "simple";
    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = super.getDetails(arguments).builder();
        builder.append(Texts.of("\n"));
        builder.append(Texts.of(TextColors.GREEN, "Members: "));
        for (User p : this.memberList) {
            builder.append(Texts.of(TextColors.RESET, p.getName() + " "));
        }
        builder.append(Texts.of("\n"));
        builder.append(Texts.of(TextColors.GOLD, "Owner permissions:\n"));
        for (Flags f : this.ownerPermissions.keySet()) {
            builder.append(Texts.of("  " + f.toString() + ": " + FGHelper.readableTristate(ownerPermissions.get(f)) + "\n"));
        }
        builder.append(Texts.of(TextColors.GREEN, "Member permissions:\n"));
        for (Flags f : this.memberPermissions.keySet()) {
            builder.append(Texts.of("  " + f.toString() + ": " + FGHelper.readableTristate(memberPermissions.get(f)) + "\n"));
        }
        builder.append(Texts.of(TextColors.RED, "Default permissions:\n"));
        for (Flags f : this.defaultPermissions.keySet()) {
            builder.append(Texts.of("  " + f.toString() + ": " + FGHelper.readableTristate(defaultPermissions.get(f)) + "\n"));
        }
        builder.append(Texts.of(TextColors.AQUA, "Passive setting: "));
        builder.append(Texts.of(TextColors.RESET, this.passiveOption.toString()));
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS MEMBERS(NAMES VARCHAR(256), USERUUID UUID);" +
                        "DELETE FROM MEMBERS;");
                try (PreparedStatement insert = conn.prepareStatement("INSERT INTO MEMBERS(NAMES, USERUUID) VALUES (?, ?)")) {
                    for (User member : memberList) {
                        insert.setString(1, member.getName());
                        insert.setObject(2, member.getUniqueId());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                statement.execute("CREATE TABLE IF NOT EXISTS MAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM MAP;");
                statement.execute("INSERT INTO MAP(KEY, VALUE) VALUES ('passive', '" + this.passiveOption.name() + "')");

                statement.execute("CREATE TABLE IF NOT EXISTS OWNERFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM OWNERFLAGMAP;");
                statement.execute("CREATE TABLE IF NOT EXISTS MEMBERFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM MEMBERFLAGMAP;");
                statement.execute("CREATE TABLE IF NOT EXISTS DEFAULTFLAGMAP(KEY VARCHAR (256), VALUE VARCHAR (256));" +
                        "DELETE FROM DEFAULTFLAGMAP;");
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO OWNERFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flags, Tristate> entry : ownerPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO MEMBERFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flags, Tristate> entry : memberPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO DEFAULTFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flags, Tristate> entry : defaultPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }
    }

    @Override
    public List<User> getMembers() {
        return this.memberList;
    }

    @Override
    public void setMembers(List<User> members) {
        this.memberList = members;
    }

    @Override
    public boolean addMember(User player) {
        return memberList.add(player);
    }

    @Override
    public boolean removeMember(User player) {
        return memberList.remove(player);
    }

    public PassiveOptions getPassiveOption() {
        return passiveOption;
    }

    public void setPassiveOption(PassiveOptions passiveOption) {
        this.passiveOption = passiveOption;
    }

    public enum PassiveOptions {
        ALLOW, DENY, PASSTHROUGH, OWNER, MEMBER, DEFAULT;

        public String toString() {
            switch (this) {
                case ALLOW:
                    return "Allow";
                case DENY:
                    return "Deny";
                case PASSTHROUGH:
                    return "Passthrough";
                case OWNER:
                    return "Owner";
                case MEMBER:
                    return "Member";
                case DEFAULT:
                    return "Default";
                default:
                    return "Awut...?";
            }
        }
    }
}
