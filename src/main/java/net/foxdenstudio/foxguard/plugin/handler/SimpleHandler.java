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
import net.foxdenstudio.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.foxguard.plugin.handler.util.Flag;
import net.foxdenstudio.foxguard.plugin.object.IMembership;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.foxdenstudio.foxcore.plugin.util.Aliases.*;

public class SimpleHandler extends OwnableHandlerBase implements IMembership {

    private final Map<Flag, Tristate> ownerPermissions;
    private final Map<Flag, Tristate> memberPermissions;
    private final Map<Flag, Tristate> defaultPermissions;
    private PassiveOptions passiveOption = PassiveOptions.PASSTHROUGH;
    private List<User> memberList = new LinkedList<>();

    public SimpleHandler(String name, int priority) {
        this(name, priority,
                new CallbackHashMap<>((o, m) -> Tristate.TRUE),
                new CallbackHashMap<>((o, m) -> Tristate.UNDEFINED),
                new CallbackHashMap<>((o, m) -> Tristate.FALSE));
    }

    public SimpleHandler(String name, int priority,
                         Map<Flag, Tristate> ownerPermissions,
                         Map<Flag, Tristate> memberPermissions,
                         Map<Flag, Tristate> defaultPermissions) {
        super(name, priority);
        this.ownerPermissions = ownerPermissions;
        this.memberPermissions = memberPermissions;
        this.defaultPermissions = defaultPermissions;
    }

    @Override
    public ProcessResult modify(String arguments, SourceState state, CommandSource source) throws CommandException {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return ProcessResult.failure();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).build();
        String[] args = parse.getArgs();
        try {
            this.lock.writeLock().lock();
            if (args.length > 0) {
                if (isAlias(GROUPS_ALIASES, args[0])) {
                    if (args.length > 1) {
                        List<User> list;
                        if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                            list = this.ownerList;
                        } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                            list = this.memberList;
                        } else {
                            return ProcessResult.of(false, Texts.of(TextColors.RED, "Not a valid group!"));
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
                                return ProcessResult.of(false, Texts.of("Not a valid operation!"));
                            }
                            if (args.length > 3) {
                                int successes = 0;
                                int failures = 0;
                                List<String> names = new ArrayList<>();
                                Collections.addAll(names, Arrays.copyOfRange(args, 3, args.length));
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
                                            if (!FCHelper.isUserOnList(list, user) && list.add(user))
                                                successes++;
                                            else failures++;
                                        }
                                        break;
                                    case REMOVE:
                                        for (User cUser : list) {
                                            if (FCHelper.isUserOnList(users, cUser)) {
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
                                return ProcessResult.of(true, Texts.of("Modified list with " + successes + " successes and " + failures + " failures."));
                            } else {
                                return ProcessResult.of(false, Texts.of("Must specify one or more users!"));
                            }
                        } else {
                            return ProcessResult.of(false, Texts.of("Must specify an operation!"));
                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify a group!"));
                    }
                } else if (isAlias(SET_ALIASES, args[0])) {
                    Map<Flag, Tristate> map;
                    if (args.length > 1) {
                        if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                            map = ownerPermissions;
                        } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                            map = memberPermissions;
                        } else if (isAlias(DEFAULT_GROUP_ALIASES, args[1])) {
                            map = defaultPermissions;
                        } else {

                            return ProcessResult.of(false, Texts.of("Not a valid group!"));
                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify a group!"));
                    }
                    if (args.length > 2) {
                        Flag flag;
                        if (args[2].equalsIgnoreCase("all")) {
                            flag = null;
                        } else {
                            flag = Flag.flagFrom(args[2]);
                            if (flag == null) {
                                return ProcessResult.of(false, Texts.of("Not a valid flag!"));
                            }
                        }
                        if (args.length > 3) {
                            if (isAlias(CLEAR_ALIASES, args[3])) {
                                if (flag == null) {
                                    map.clear();
                                    return ProcessResult.of(true, Texts.of("Successfully cleared flags!"));
                                } else {
                                    map.remove(flag);
                                    return ProcessResult.of(true, Texts.of("Successfully cleared flag!"));
                                }
                            } else {
                                Tristate tristate = tristateFrom(args[3]);
                                if (tristate == null) {
                                    return ProcessResult.of(false, Texts.of("Not a valid value!"));
                                }
                                if (flag == null) {
                                    for (Flag thatExist : Flag.values()) {
                                        map.put(thatExist, tristate);
                                    }
                                    return ProcessResult.of(true, Texts.of("Successfully set flags!"));
                                } else {
                                    map.put(flag, tristate);
                                    return ProcessResult.of(true, Texts.of("Successfully set flag!"));
                                }
                            }
                        } else {
                            return ProcessResult.of(false, Texts.of("Must specify a value!"));
                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify a flag!"));
                    }
                } else if (isAlias(PASSIVE_ALIASES, args[0])) {
                    if (args.length > 1) {
                        if (isAlias(TRUE_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.ALLOW;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else if (isAlias(FALSE_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.DENY;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else if (isAlias(PASSTHROUGH_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.PASSTHROUGH;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else if (isAlias(OWNER_GROUP_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.OWNER;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else if (isAlias(MEMBER_GROUP_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.MEMBER;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else if (isAlias(DEFAULT_GROUP_ALIASES, args[1])) {
                            this.passiveOption = PassiveOptions.DEFAULT;
                            return ProcessResult.of(true, Texts.of("Successfully set passive option!"));
                        } else {
                            return ProcessResult.of(false, Texts.of("Not a valid option!"));
                        }
                    } else {
                        return ProcessResult.of(false, Texts.of("Must specify an option!"));
                    }
                } else {
                    return ProcessResult.of(false, Texts.of("Not a valid SimpleHandler command!"));
                }
            } else {
                return ProcessResult.of(false, Texts.of("Must specify a command!"));
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public Tristate handle(User user, Flag flag, Event event) {
        try {
            this.lock.readLock().lock();
            if (!isEnabled) return Tristate.UNDEFINED;
            if (user == null) {
                switch (this.passiveOption) {
                    case OWNER:
                        return getResult(this.ownerPermissions, flag);
                    case MEMBER:
                        return getResult(this.memberPermissions, flag);
                    case DEFAULT:
                        return getResult(this.defaultPermissions, flag);
                    case ALLOW:
                        return Tristate.TRUE;
                    case DENY:
                        return Tristate.FALSE;
                    case PASSTHROUGH:
                        return Tristate.UNDEFINED;
                }
            }
            if (FCHelper.isUserOnList(this.ownerList, user)) return getResult(this.ownerPermissions, flag);
            else if (FCHelper.isUserOnList(this.memberList, user)) return getResult(this.memberPermissions, flag);
            else return getResult(this.defaultPermissions, flag);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private Tristate getResult(Map<Flag, Tristate> map, Flag flag) {
        Flag temp = flag;
        while (temp != null && !map.containsKey(temp)) {
            temp = temp.getParent();
        }
        if (temp != null) return map.get(temp);
        else return map.get(flag);
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
        builder.append(Texts.of(TextColors.GREEN,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " group members add "),
                TextActions.showText(Texts.of("Click to Add a Player(s) to Members")),
                "Members: "));
        try {
            this.lock.readLock().lock();
            for (User u : this.memberList) {
                builder.append(Texts.of(TextColors.RESET,
                        TextActions.suggestCommand("/foxguard modify handler " + this.name + " group members remove " + u.getName()),
                        TextActions.showText(Texts.of("Click to Remove Player \"" + u.getName() + "\" from Members")),
                        u.getName())).append(Texts.of("  "));
            }
            builder.append(Texts.of("\n"));
            builder.append(Texts.of(TextColors.GOLD,
                    TextActions.suggestCommand("/foxguard modify handler " + this.name + " set owners "),
                    TextActions.showText(Texts.of("Click to Set a Flag")),
                    "Owner permissions:\n"));
            for (Flag f : this.ownerPermissions.keySet()) {
                builder.append(
                        Texts.builder().append(Texts.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(ownerPermissions.get(f)))
                                .append(Texts.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set owners " + f.flagName() + " "))
                                .onHover(TextActions.showText(Texts.of("Click to Change This Flag")))
                                .build()
                );
            }
            builder.append(Texts.of(TextColors.GREEN,
                    TextActions.suggestCommand("/foxguard modify handler " + this.name + " set members "),
                    TextActions.showText(Texts.of("Click to Set a Flag")),
                    "Member permissions:\n"));
            for (Flag f : this.memberPermissions.keySet()) {
                builder.append(
                        Texts.builder().append(Texts.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(memberPermissions.get(f)))
                                .append(Texts.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set members " + f.flagName() + " "))
                                .onHover(TextActions.showText(Texts.of("Click to Change This Flag")))
                                .build()
                );
            }
            builder.append(Texts.of(TextColors.RED,
                    TextActions.suggestCommand("/foxguard modify handler " + this.name + " set default "),
                    TextActions.showText(Texts.of("Click to Set a Flag")),
                    "Default permissions:\n"));
            for (Flag f : this.defaultPermissions.keySet()) {
                builder.append(
                        Texts.builder().append(Texts.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(defaultPermissions.get(f)))
                                .append(Texts.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set default " + f.flagName() + " "))
                                .onHover(TextActions.showText(Texts.of("Click to Change This Flag")))
                                .build()
                );
            }
            builder.append(Texts.builder()
                            .append(Texts.of(TextColors.AQUA, "Passive setting: "))
                            .append(Texts.of(TextColors.RESET, this.passiveOption.toString()))
                            .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " passive "))
                            .onHover(TextActions.showText(Texts.of("Click to Change Passive Setting"))).build()
            );
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
                for (Map.Entry<Flag, Tristate> entry : ownerPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO MEMBERFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : memberPermissions.entrySet()) {
                    statement.setString(1, entry.getKey().name());
                    statement.setString(2, entry.getValue().name());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = conn.prepareStatement("INSERT INTO DEFAULTFLAGMAP(KEY, VALUE) VALUES (? , ?)")) {
                for (Map.Entry<Flag, Tristate> entry : defaultPermissions.entrySet()) {
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
        return "/foxguard modify handler " + this.getName() + " group owners add ";
    }

    @Override
    protected String getRemoveOwnerSuggestion(User user) {
        return "/foxguard modify handler " + this.getName() + " group owners remove " + user.getName();
    }

    @Override
    public List<User> getMembers() {
        try {
            this.lock.readLock().lock();
            return this.memberList;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void setMembers(List<User> members) {
        try {
            this.lock.writeLock().lock();
            this.memberList = members;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addMember(User player) {
        try {
            this.lock.writeLock().lock();
            return memberList.add(player);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeMember(User player) {
        try {
            this.lock.writeLock().lock();
            return memberList.remove(player);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public PassiveOptions getPassiveOption() {
        try {
            this.lock.readLock().lock();
            return passiveOption;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void setPassiveOption(PassiveOptions passiveOption) {
        try {
            this.lock.writeLock().lock();
            this.passiveOption = passiveOption;
        } finally {
            this.lock.writeLock().unlock();
        }
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
