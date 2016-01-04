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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Flag;
import org.spongepowered.api.Sponge;
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

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class PassiveHandler extends OwnableHandlerBase {

    private final Map<Flag, Tristate> map;

    public PassiveHandler(String name, int priority) {
        this(name, priority, new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED));
    }

    public PassiveHandler(String name, int priority, CallbackHashMap<Flag, Tristate> map) {
        super(name, priority);
        this.map = map;
    }

    @Override
    public Tristate handle(@Nullable User user, Flag flag, Event event) {
        try {
            this.lock.readLock().lock();
            if (!isEnabled || user != null) {
                return Tristate.UNDEFINED;
            }
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
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.handlers")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return ProcessResult.failure();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).parse();
        try {
            this.lock.writeLock().lock();
            if (parse.args.length > 0) {
                if (isIn(OWNER_GROUP_ALIASES, parse.args[0])) {
                    if (parse.args.length > 1) {
                        UserOperations op;
                        if (parse.args[1].equalsIgnoreCase("add")) {
                            op = UserOperations.ADD;
                        } else if (parse.args[1].equalsIgnoreCase("remove")) {
                            op = UserOperations.REMOVE;
                        } else if (parse.args[1].equalsIgnoreCase("set")) {
                            op = UserOperations.SET;
                        } else {
                            return ProcessResult.of(false, Text.of("Not a valid operation!"));
                        }
                        if (parse.args.length > 2) {
                            int successes = 0;
                            int failures = 0;
                            List<String> names = new ArrayList<>();
                            Collections.addAll(names, Arrays.copyOfRange(parse.args, 2, parse.args.length));
                            List<User> argUsers = new ArrayList<>();
                            for (String name : names) {
                                Optional<User> optUser = FoxGuardMain.instance().getUserStorage().get(name);
                                if (optUser.isPresent() && !FCHelper.isUserOnList(argUsers, optUser.get()))
                                    argUsers.add(optUser.get());
                                else failures++;
                            }
                            switch (op) {
                                case ADD:
                                    for (User user : argUsers) {
                                        if (!FCHelper.isUserOnList(ownerList, user) && ownerList.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User user : argUsers) {
                                        if (FCHelper.isUserOnList(ownerList, user)) {
                                            ownerList.remove(user);
                                            successes++;
                                        } else failures++;
                                    }
                                    break;
                                case SET:
                                    ownerList.clear();
                                    for (User user : argUsers) {
                                        ownerList.add(user);
                                        successes++;
                                    }
                            }
                            return ProcessResult.of(true, Text.of("Modified list with " + successes + " successes and " + failures + " failures."));
                        } else {
                            return ProcessResult.of(false, Text.of("Must specify one or more users!"));
                        }
                    } else {
                        return ProcessResult.of(false, Text.of("Must specify an operation!"));
                    }

                } else if (isIn(SET_ALIASES, parse.args[0])) {
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
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("set", "owners").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(OWNER_GROUP_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return Arrays.stream(Flag.values())
                            .map(Flag::flagName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                if (isIn(OWNER_GROUP_ALIASES, parse.args[0])) {
                    if(parse.args[1].equalsIgnoreCase("set")){
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[1].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCHelper.isUserOnList(this.ownerList, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[1].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCHelper.isUserOnList(this.ownerList, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index > 2) {
                if (isIn(OWNER_GROUP_ALIASES, parse.args[0])) {
                    if (parse.args[1].equalsIgnoreCase("set")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[1].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCHelper.isUserOnList(this.ownerList, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[1].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCHelper.isUserOnList(this.ownerList, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public Text getDetails(String arguments) {
        Text.Builder builder = super.getDetails(arguments).toBuilder();
        builder.append(Text.of("\n"));
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard modify handler " + this.name + " set "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Passive Flags:\n"));
        try {
            this.lock.readLock().lock();
            for (Flag f : this.map.keySet()) {
                builder.append(
                        Text.builder().append(Text.of("  " + f.toString() + ": "))
                                .append(FCHelper.readableTristateText(map.get(f)))
                                .append(Text.of("\n"))
                                .onClick(TextActions.suggestCommand("/foxguard modify handler " + this.name + " set " + f.flagName() + " "))
                                .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
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
