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
import net.foxdenstudio.sponge.foxguard.plugin.FGStorageManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagObject;
import net.foxdenstudio.sponge.foxguard.plugin.flag.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class BasicHandler extends HandlerBase {

    public static final String[] INDEX_ALIASES = {"index", "i"};
    public static final String[] COLOR_ALIASES = {"color", "colour", "col", "c"};
    public static final String[] DISPLAY_NAME_ALIASES = {"displayname", "display", "disp", "dispname", "title", "d"};

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(INDEX_ALIASES, key) && !map.containsKey("index")) {
            map.put("index", value);
        } else if (isIn(COLOR_ALIASES, key) && !map.containsKey("color")) {
            map.put("color", value);
        } else if (isIn(DISPLAY_NAME_ALIASES, key) && !map.containsKey("displayname")) {
            map.put("displayname", value);
        }
    };

    private final List<Group> groups;
    private final Map<Group, List<Entry>> groupPermissions;
    private final List<Entry> defaultPermissions;

    private final Map<Group, Map<FlagBitSet, Tristate>> groupPermCache;
    private final Map<User, Map<FlagBitSet, Tristate>> userPermCache;
    private final Map<FlagBitSet, Tristate> defaultPermCache;

    private PassiveOptions passiveOption = PassiveOptions.PASSTHROUGH;
    private Group passiveGroup;
    private Map<FlagBitSet, Tristate> passiveGroupCacheRef;
    private final Map<FlagBitSet, Tristate> passivePermCache;

    public BasicHandler(String name, int priority) {
        this(name, priority,
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>());
    }

    public BasicHandler(String name, int priority,
                        List<Group> groups,
                        Map<Group, List<Entry>> groupPermissions,
                        List<Entry> defaultPermissions) {
        super(name, priority);
        this.groups = groups;

        this.groupPermissions = groupPermissions;
        this.defaultPermissions = defaultPermissions;

        this.groupPermCache = new CacheMap<>((k1, m1) -> {
            if (k1 instanceof Group) {
                List<Entry> entries = BasicHandler.this.groupPermissions.get(k1);
                Map<FlagBitSet, Tristate> map = new CacheMap<>((k2, m2) -> {
                    if (k2 instanceof FlagBitSet) {
                        FlagBitSet flags = (FlagBitSet) k2;
                        Tristate state = Tristate.UNDEFINED;
                        for (Entry entry : entries) {
                            if (flags.toFlagSet().containsAll(entry.set)) {
                                state = entry.state;
                                break;
                            }
                        }
                        m2.put(flags, state);
                        return state;
                    } else return null;
                });
                m1.put((Group) k1, map);
                return map;
            } else return null;
        });
        this.defaultPermCache = new CacheMap<>((k, m) -> {
            if (k instanceof FlagBitSet) {
                FlagBitSet flags = (FlagBitSet) k;
                Tristate state = Tristate.UNDEFINED;
                for (Entry entry : BasicHandler.this.defaultPermissions) {
                    if (flags.toFlagSet().containsAll(entry.set)) {
                        state = entry.state;
                        break;
                    }
                }
                m.put(flags, state);
                return state;
            } else return null;
        });
        this.userPermCache = new CacheMap<>((k, m) -> {
            if (k instanceof User) {
                for (Group g : groups) {
                    if (FCUtil.isUserInCollection(g.users, (User) k)) {
                        Map<FlagBitSet, Tristate> map = groupPermCache.get(g);
                        m.put(((User) k), map);
                        return map;
                    }
                }
                m.put((User) k, defaultPermCache);
                return defaultPermCache;
            } else return null;
        });
        this.passivePermCache = new CacheMap<>((k, m) -> {
            if (k instanceof FlagBitSet) {
                FlagBitSet flags = (FlagBitSet) k;
                Tristate state = Tristate.UNDEFINED;
                switch (passiveOption) {
                    case ALLOW:
                        state = Tristate.TRUE;
                        break;
                    case DENY:
                        state = Tristate.FALSE;
                        break;
                    case GROUP:
                        state = passiveGroupCacheRef.get(flags);
                        break;
                    case DEFAULT:
                        state = defaultPermCache.get(flags);
                        break;
                }
                m.put(flags, state);
                return state;
            } else return null;
        });
    }

    /* @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            if (isIn(GROUPS_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    Set<User> set;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        set = this.owners;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        set = this.members;
                    } else {
                        return ProcessResult.of(false, Text.of(TextColors.RED, "Not a valid group!"));
                    }
                    if (parse.args.length > 2) {
                        Operation op;
                        if (parse.args[2].equalsIgnoreCase("add")) {
                            op = Operation.ADD;
                        } else if (parse.args[2].equalsIgnoreCase("remove")) {
                            op = Operation.REMOVE;
                        } else if (parse.args[2].equalsIgnoreCase("set")) {
                            op = Operation.SET;
                        } else {
                            return ProcessResult.of(false, Text.of("Not a valid operation!"));
                        }
                        if (parse.args.length > 3) {
                            int successes = 0;
                            int failures = 0;
                            List<String> names = new ArrayList<>();
                            Collections.addAll(names, Arrays.copyOfRange(parse.args, 3, parse.args.length));
                            List<User> argUsers = new ArrayList<>();
                            for (String name : names) {
                                Optional<User> optUser = FoxGuardMain.instance().getUserStorage().get(name);
                                if (optUser.isPresent() && !FCUtil.isUserInCollection(argUsers, optUser.get()))
                                    argUsers.add(optUser.get());
                                else failures++;
                            }
                            switch (op) {
                                case ADD:
                                    for (User user : argUsers) {
                                        if (!FCUtil.isUserInCollection(set, user) && set.add(user))
                                            successes++;
                                        else failures++;
                                    }
                                    break;
                                case REMOVE:
                                    for (User user : argUsers) {
                                        if (FCUtil.isUserInCollection(set, user)) {
                                            set.removeIf(u -> u.getUniqueId().equals(user.getUniqueId()));
                                            successes++;
                                        } else failures++;
                                    }
                                    break;
                                case SET:
                                    set.clear();
                                    for (User user : argUsers) {
                                        set.add(user);
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
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a group!"));
                }
            } else if (isIn(SET_ALIASES, parse.args[0])) {
                Map<IFlag, Tristate> map;
                if (parse.args.length > 1) {
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        map = ownerPermissions;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        map = memberPermissions;
                    } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                        map = defaultPermissions;
                    } else {

                        return ProcessResult.of(false, Text.of("Not a valid group!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a group!"));
                }
                if (parse.args.length > 2) {
                    IFlag flag;
                    if (parse.args[2].equalsIgnoreCase("all")) {
                        flag = null;
                    } else {
                        flag = Flag.flagFrom(parse.args[2]);
                        if (flag == null) {
                            return ProcessResult.of(false, Text.of("Not a valid flag!"));
                        }
                    }
                    if (parse.args.length > 3) {
                        if (isIn(CLEAR_ALIASES, parse.args[3])) {
                            if (flag == null) {
                                map.clear();
                                clearCache();
                                return ProcessResult.of(true, Text.of("Successfully cleared flags!"));
                            } else {
                                map.remove(flag);
                                clearCache();
                                return ProcessResult.of(true, Text.of("Successfully cleared flag!"));
                            }
                        } else {
                            Tristate tristate = tristateFrom(parse.args[3]);
                            if (tristate == null) {
                                return ProcessResult.of(false, Text.of("Not a valid value!"));
                            }
                            if (flag == null) {
                                for (IFlag thatExist : Flag.getFlags()) {
                                    map.put(thatExist, tristate);
                                }
                                clearCache();
                                return ProcessResult.of(true, Text.of("Successfully set flags!"));
                            } else {
                                map.put(flag, tristate);
                                clearCache();
                                return ProcessResult.of(true, Text.of("Successfully set flag!"));
                            }
                        }
                    } else {
                        return ProcessResult.of(false, Text.of("Must specify a value!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a flag!"));
                }
            } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    if (isIn(TRUE_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.ALLOW;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.DENY;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(PASSTHROUGH_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.PASSTHROUGH;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.OWNER;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.MEMBER;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.DEFAULT;
                        return ProcessResult.of(true, Text.of("Successfully set passive option!"));
                    } else {
                        return ProcessResult.of(false, Text.of("Not a valid option!"));
                    }
                } else {
                    return ProcessResult.of(false, Text.of("Must specify an option!"));
                }
            } else {
                return ProcessResult.of(false, Text.of("Not a valid SimpleHandler command!"));
            }
        } else {
            return ProcessResult.of(false, Text.of("Must specify a command!"));
        }
    }*/

    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify a command!"));

        if (isIn(GROUPS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an operation!"));

            if (parse.args[1].equalsIgnoreCase("add")) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                if (!isNameValid(parse.args[2]))
                    return ProcessResult.of(false, Text.of("Not a valid group name!"));
                Optional<Group> groupOptional = createGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("Group already exists with this name!"));

                Group group = groupOptional.get();
                if (parse.flags.containsKey("color")) {
                    String colorString = parse.flags.get("color");
                    Optional<TextColor> colorOptional = FCUtil.textColorFromHex(colorString);
                    if (!colorOptional.isPresent())
                        colorOptional = FCUtil.textColorFromName(colorString);
                    if (colorOptional.isPresent()) group.color = colorOptional.get();
                }
                if (parse.flags.containsKey("displayname")) {
                    String name = parse.flags.get("displayname");
                    if (!name.isEmpty()) {
                        group.displayName = name;
                    }
                }
                if (parse.flags.containsKey("index")) {
                    String number = parse.flags.get("index");
                    if (!number.isEmpty()) {
                        try {
                            int index = Integer.parseInt(number);
                            moveGroup(group, index);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return ProcessResult.of(true, Text.of("Successfully made group!"));
            } else if (parse.args[1].equalsIgnoreCase("remove")) {
                if (parse.args.length <= 2)
                    return ProcessResult.of(false, Text.of("Must specify a group to remove!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("Not a valid group!"));

                removeGroup(groupOptional.get());
                return ProcessResult.of(true, Text.of("Successfully removed group!"));
            } else if (parse.args[1].equalsIgnoreCase("modify")) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));

                Group group = groupOptional.get();
                if (parse.flags.containsKey("color")) {
                    String colorString = parse.flags.get("color");
                    if (!colorString.isEmpty()) {
                        Optional<TextColor> colorOptional = FCUtil.textColorFromHex(colorString);
                        if (!colorOptional.isPresent())
                            colorOptional = FCUtil.textColorFromName(colorString);
                        if (colorOptional.isPresent()) group.color = colorOptional.get();
                    } else {
                        group.color = TextColors.WHITE;
                    }
                }
                if (parse.flags.containsKey("displayname")) {
                    String name = parse.flags.get("displayname");
                    if (!name.isEmpty()) {
                        group.displayName = name;
                    } else group.displayName = group.name;
                }
                if (parse.flags.containsKey("index")) {
                    String number = parse.flags.get("index");
                    if (!number.isEmpty()) {
                        try {
                            int index = Integer.parseInt(number);
                            moveGroup(group, index);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return ProcessResult.of(true, Text.of("Successfully modified group!"));
            } else if (parse.args[1].equalsIgnoreCase("rename")) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));
                if (parse.args.length < 4)
                    return ProcessResult.of(false, Text.of("Must specify a new group name!"));
                if (!isNameValid(parse.args[3]))
                    return ProcessResult.of(false, Text.of("New group name is not valid!"));
                renameGroup(groupOptional.get(), parse.args[3]);
                return ProcessResult.of(true, Text.of("Successfully renamed group!"));
            } else if (parse.args[1].equalsIgnoreCase("move")) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                Group group;
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));
                else group = groupOptional.get();
                if (parse.args.length < 4)
                    return ProcessResult.of(false, Text.of("Must specify a target index or direction!"));
                String target = parse.args[3];
                final int currentIndex = this.groups.indexOf(group);
                int newIndex;
                if (!target.isEmpty()) {
                    if (target.equalsIgnoreCase("up")) {
                        newIndex = currentIndex - 1;
                    } else if (target.equalsIgnoreCase("down")) {
                        newIndex = currentIndex + 1;
                    } else if (target.equalsIgnoreCase("top") || target.equalsIgnoreCase("first")) {
                        newIndex = 0;
                    } else if (target.equalsIgnoreCase("bottom") || target.equalsIgnoreCase("last")) {
                        newIndex = this.groups.size() - 1;
                    } else if (target.matches("[0-9~]+")) {
                        try {
                            newIndex = FCUtil.parseCoordinate(currentIndex, target) - 1;
                        } catch (NumberFormatException ignored) {
                            return ProcessResult.of(false, Text.of("Target index number is not formatted correctly!"));
                        }
                    } else {
                        return ProcessResult.of(false, Text.of("Not a valid target position!"));
                    }
                    moveGroup(group, newIndex);
                }
                return ProcessResult.of(true, Text.of("Successfully moved group!"));
            } else return ProcessResult.of(false, Text.of("Not a valid operation!"));
        } else if (isIn(USERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an operation!"));
            Operation op;
            if (parse.args[1].equalsIgnoreCase("add")) {
                op = Operation.ADD;
            } else if (parse.args[1].equalsIgnoreCase("remove")) {
                op = Operation.REMOVE;
            } else if (parse.args[1].equalsIgnoreCase("set")) {
                op = Operation.SET;
            } else {
                return ProcessResult.of(false, Text.of("Not a valid operation!"));
            }
            if (parse.args.length < 3) return ProcessResult.of(false, Text.of("Must specify a group!"));
            String[] newArgs = Arrays.copyOfRange(parse.args, 2, parse.args.length);
            Set<Group> groups = new HashSet<>();
            Set<User> users = new HashSet<>();
            boolean multi = false;
            for (String s : newArgs) {
                if (s.contains(">")) {
                    multi = true;
                    break;
                }
            }
            int successes = 0, failures = 0;
            if (multi) {

            } else {
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                Group group;
                if (!groupOptional.isPresent()) {
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));
                } else {
                    group = groupOptional.get();
                    groups.add(group);
                }
                if (parse.args.length < 4) {
                    if (op != Operation.SET) return ProcessResult.of(false, Text.of("Must specify a user!"));
                    else {
                        group.users.clear();
                        return ProcessResult.of(true, Text.of("Successfully cleared group!"));
                    }
                }
                newArgs = Arrays.copyOfRange(parse.args, 3, parse.args.length);
                for (String s : newArgs) {
                    try {
                        UUID uuid = UUID.fromString(s);
                        Optional<User> userOptional = FoxGuardMain.instance().getUserStorage().get(uuid);
                        if (userOptional.isPresent()) {
                            users.add(userOptional.get());
                        } else if (newArgs.length == 1) {
                            return ProcessResult.of(false, Text.of("No user exists with this UUID!"));
                        } else {
                            failures++;
                        }
                    } catch (IllegalArgumentException e) {
                        Optional<User> userOptional = FoxGuardMain.instance().getUserStorage().get(s);
                        if (userOptional.isPresent()) {
                            users.add(userOptional.get());
                        } else if (newArgs.length == 1) {
                            return ProcessResult.of(false, Text.of("No user exists with this name!"));
                        } else {
                            failures++;
                        }
                    }
                }
            }
            switch (op) {
                case ADD:
                    for (Group g : groups) {
                        for (User u : users) {
                            if (!FCUtil.isUserInCollection(g.users, u)) {
                                g.users.add(u);
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                    }
                    break;
                case REMOVE:
                    for (Group g : groups) {
                        for (User u : users) {
                            User user = null;
                            for (User scan : g.users) {
                                if (u.getUniqueId().equals(scan.getUniqueId()))
                                    user = scan;
                            }
                            if (user != null) {
                                g.users.remove(user);
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                    }
                    break;
                case SET:
                    for (Group g : groups) {
                        g.users.clear();
                        for (User u : users) {
                            if (g.users.add(u)) {
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                    }
                    break;
            }
            if (failures == 0) {
                if (successes == 1)
                    return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " user!"));
                else {
                    return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " " + successes + " users!"));
                }
            } else {
                if (successes > 0) {
                    return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " " + successes + " users with " + failures + " failures!"));
                } else {
                    return ProcessResult.of(false, Text.of("Failed to " + op.pastTense + " " + failures + " users!"));
                }
            }
        } else if (isIn(SET_ALIASES, parse.args[0])) {

        } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an option!"));

            if (isIn(TRUE_ALIASES, parse.args[1])) {
                this.passiveOption = PassiveOptions.ALLOW;
            } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                this.passiveOption = PassiveOptions.DENY;
            } else if (isIn(PASSTHROUGH_ALIASES, parse.args[1])) {
                this.passiveOption = PassiveOptions.PASSTHROUGH;
            } else if (isIn(GROUPS_ALIASES, parse.args[1])) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));

                this.passiveOption = PassiveOptions.GROUP;
                passiveGroup = groupOptional.get();
            } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                this.passiveOption = PassiveOptions.DEFAULT;
            } else {
                return ProcessResult.of(false, Text.of("Not a valid option!"));
            }
            return ProcessResult.of(true, Text.of("Successfully set passive option!"));
        }
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.args.length == 0) {
                return ImmutableList.of("groups", "users", "set", "passive").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.args.length == 1) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "modify", "rename", "move").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(USERS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("allow", "deny", "pass", "group", "default").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.args.length == 2) {

            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY)) {

        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {

        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    /*@Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)1
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("set", "group", "passive").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("owner", "member").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("owner", "member").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "owner", "member", "default").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return Flag.getFlags().stream()
                            .map(IFlag::flagName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 3) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    Set<User> set;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        set = this.owners;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        set = this.members;
                    } else {
                        return ImmutableList.of();
                    }
                    if (parse.args[2].equalsIgnoreCase("set")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCUtil.isUserInCollection(set, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCUtil.isUserInCollection(set, player))
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(SET_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "clear").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index > 3) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    Set<User> set;
                    if (isIn(OWNER_GROUP_ALIASES, parse.args[1])) {
                        set = this.owners;
                    } else if (isIn(MEMBER_GROUP_ALIASES, parse.args[1])) {
                        set = this.members;
                    } else {
                        return ImmutableList.of();
                    }
                    if (parse.args[2].equalsIgnoreCase("set")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("add")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> !FCUtil.isUserInCollection(set, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else if (parse.args[2].equalsIgnoreCase("remove")) {
                        return Sponge.getGame().getServer().getOnlinePlayers().stream()
                                .filter(player -> FCUtil.isUserInCollection(set, player))
                                .map(Player::getName)
                                .filter(alias -> !isIn(Arrays.copyOfRange(parse.args, 2, parse.args.length), alias))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }*/

    @Override
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        if (user == null) return EventResult.of(this.passivePermCache.get(flags));
        else return EventResult.of(this.userPermCache.get(user).get(flags));
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Optional<Event> event, Object... extra) {
        return EventResult.pass();
    }

    @Override
    public String getShortTypeName() {
        return "Basic";
    }

    @Override
    public String getLongTypeName() {
        return "Basic";
    }

    @Override
    public String getUniqueTypeString() {
        return "basic";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        Text.Builder passiveBuilder = Text.builder()
                .append(Text.of(TextColors.AQUA, "Passive setting: "))
                .append(Text.of(TextColors.RESET, this.passiveOption.toString()));
        if (this.passiveOption == PassiveOptions.GROUP)
            passiveBuilder.append(Text.of(passiveGroup.color, passiveGroup.displayName));
        passiveBuilder
                .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " passive "))
                .onHover(TextActions.showText(Text.of("Click to Change Passive Setting")));
        builder.append(passiveBuilder.build());
        builder.append(Text.NEW_LINE);
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.getName() + " group add "),
                TextActions.showText(Text.of("Click to Add a Group")),
                "----- Group Members -----\n"));
        for (Group group : groups) {
            builder.append(Text.of(group.color,
                    TextActions.suggestCommand("/foxguard md h " + this.getName() + " user add " + group.name + " "),
                    TextActions.showText(Text.of("Click to Add a Player(s) to \"", group.color, group.displayName, TextColors.RESET, "\" (" + group.name + ")")),
                    group.displayName,
                    TextColors.RESET, ": "));
            for (User u : group.users) {
                TextColor color = TextColors.WHITE;
                if (source instanceof Player && ((Player) source).getUniqueId().equals(u.getUniqueId()))
                    color = TextColors.YELLOW;
                builder.append(Text.of(TextColors.RESET,
                        color,
                        TextActions.suggestCommand("/foxguard md h " + this.getName() + " user remove " + group.name + " " + u.getName()),
                        TextActions.showText(Text.of("Click to Remove Player \"" + u.getName() + "\" from \"", group.color, group.displayName, TextColors.RESET, "\" (" + group.name + ")")),
                        u.getName())).append(Text.of(" "));
            }
            builder.append(Text.NEW_LINE);
        }
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.getName() + " group owners add "),
                TextActions.showText(Text.of("Click to Add a Group")),
                "----- Group Permissions -----\n"));
        for (Group group : groups) {
            builder.append(Text.of(group.color,
                    TextActions.suggestCommand("/foxguard md h " + this.name + " set " + group.name + " "),
                    TextActions.showText(Text.of("Click to Set a Flag")),
                    group.displayName + ":\n"));
            for (Entry entry : this.groupPermissions.get(group)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (FlagObject flag : entry.set) {
                    stringBuilder.append(flag.name).append(" ");
                }
                builder.append(Text.of(stringBuilder.toString(), TextColors.AQUA, ": "))
                        .append(FCUtil.readableTristateText(entry.state))
                        .append(Text.NEW_LINE);
            }
        }
        builder.append(Text.of(TextColors.RED,
                TextActions.suggestCommand("/foxguard md h " + this.name + " set default "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Default:"));
        for (Entry entry : this.defaultPermissions) {
            StringBuilder stringBuilder = new StringBuilder();
            for (FlagObject flag : entry.set) {
                stringBuilder.append(flag.name).append(" ");
            }
            builder.append(Text.NEW_LINE)
                    .append(Text.of(stringBuilder.toString(), TextColors.AQUA, ": "))
                    .append(FCUtil.readableTristateText(entry.state));
        }
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    /*@Override
    public void save(Path directory) {
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
            Map<String, String> ownerStorageFlagMap = flagMapDB.hashMap("owners", Serializer.STRING, Serializer.STRING).createOrOpen();
            ownerStorageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : ownerPermissions.entrySet()) {
                ownerStorageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
            Map<String, String> memberStorageFlagMap = flagMapDB.hashMap("members", Serializer.STRING, Serializer.STRING).createOrOpen();
            memberStorageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : memberPermissions.entrySet()) {
                memberStorageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
            Map<String, String> defaultStorageFlagMap = flagMapDB.hashMap("default", Serializer.STRING, Serializer.STRING).createOrOpen();
            defaultStorageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : defaultPermissions.entrySet()) {
                defaultStorageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
            Atomic.String passiveOptionString = flagMapDB.atomicString("passive").createOrOpen();
            passiveOptionString.set(passiveOption.name());
        }
        Path usersFile = directory.resolve("users.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(usersFile).build();
        if (Files.exists(usersFile)) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        List<CommentedConfigurationNode> ownerNodes = new ArrayList<>();
        for (User user : owners) {
            CommentedConfigurationNode node = loader.createEmptyNode(ConfigurationOptions.defaults());
            node.getNode("username").setValue(user.getName());
            node.getNode("uuid").setValue(user.getUniqueId().toString());
            ownerNodes.add(node);
        }
        root.getNode("owners").setValue(ownerNodes);
        List<CommentedConfigurationNode> memberNodes = new ArrayList<>();
        for (User user : members) {
            CommentedConfigurationNode node = loader.createEmptyNode(ConfigurationOptions.defaults());
            node.getNode("username").setValue(user.getName());
            node.getNode("uuid").setValue(user.getUniqueId().toString());
            memberNodes.add(node);
        }
        root.getNode("members").setValue(memberNodes);
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void save(Path directory) {
    }

    public Optional<Group> createGroup(String name) {
        if (!isNameValid(name.toLowerCase())) return Optional.empty();
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return Optional.empty();
        }
        Group group = new Group(name);
        this.groups.add(group);
        this.groupPermissions.put(group, new ArrayList<>());
        return Optional.of(group);
    }

    public Optional<Group> getGroup(String name) {
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return Optional.of(g);
        }
        return Optional.empty();
    }

    public Optional<Group> getOrCreateGroup(String name) {
        if (!isNameValid(name.toLowerCase())) return Optional.empty();
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return Optional.of(g);
        }
        Group group = new Group(name);
        this.groups.add(group);
        this.groupPermissions.put(group, new ArrayList<>());
        return Optional.of(group);
    }

    public boolean groupExists(String name) {
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public boolean removeGroup(Group group) {
        this.groupPermissions.remove(group);
        return this.groups.remove(group);
    }

    public boolean removeGroup(String name) {
        Optional<Group> groupOptional = getGroup(name);
        if (groupOptional.isPresent()) {
            return removeGroup(groupOptional.get());
        } else {
            return false;
        }
    }

    public boolean moveGroup(Group group, int index) {
        if (this.groups.contains(group)) {
            this.groups.remove(group);
            if (index < 0) index = 0;
            if (index > this.groups.size()) index = this.groups.size();
            this.groups.add(index, group);
            return true;
        } else return false;
    }

    public boolean renameGroup(Group group, String newName) {
        if (this.groups.contains(group)) {
            if (groupExists(newName)) return false;
            group.name = newName.toLowerCase();
            return true;
        } else return false;
    }

    public PassiveOptions getPassiveOption() {
        return passiveOption;
    }

    public void setPassiveOption(PassiveOptions passiveOption) {
        this.passiveOption = passiveOption;
    }

    private void clearCache() {
        this.groupPermCache.clear();
        this.defaultPermCache.clear();
        this.userPermCache.clear();
    }

    public static boolean isNameValid(String name) {
        if (name.matches("^.*[ :\\.=;\"\'\\\\/\\{\\}\\(\\)\\[\\]<>#@\\|\\?\\*].*$")) return false;
        if (name.equalsIgnoreCase("default")) return false;
        for (String s : FGStorageManager.FS_ILLEGAL_NAMES) {
            if (name.equalsIgnoreCase(s)) return false;
        }
        return true;
    }

    public enum PassiveOptions {
        ALLOW, DENY, PASSTHROUGH, GROUP, DEFAULT;

        public String toString() {
            switch (this) {
                case ALLOW:
                    return "Allow";
                case DENY:
                    return "Deny";
                case PASSTHROUGH:
                    return "Passthrough";
                case GROUP:
                    return "Group : ";
                case DEFAULT:
                    return "Default";
                default:
                    return "Awut...?";
            }
        }
    }

    private enum Operation {
        ADD("added"),
        REMOVE("removed"),
        SET("set");

        public final String pastTense;

        Operation(String pastTense) {
            this.pastTense = pastTense;
        }
    }

    public static class Entry {
        public Set<FlagObject> set;
        public Tristate state;

        public Entry(Set<FlagObject> set, Tristate state) {
            this.set = set;
            this.state = state;
        }
    }

    public static class Group {
        private String name;
        private String displayName;
        private TextColor color;
        private final Set<User> users;

        private Group(String name) {
            this(name, new HashSet<>());
        }

        private Group(String name, Set<User> users) {
            this(name, users, TextColors.WHITE);
        }

        private Group(String name, Set<User> users, TextColor color) {
            this.name = name.toLowerCase();
            this.displayName = name;
            this.color = color;
            this.users = users;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public TextColor getColor() {
            return color;
        }

        public void setColor(TextColor color) {
            this.color = color;
        }

        public Set<User> getUsers() {
            return users;
        }
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] basicAliases = {"basic", "base"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) {
            BasicHandler handler = new BasicHandler(name, priority);
//            if (source instanceof Player) handler.addOwner((Player) source);
            return handler;
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            BasicHandler handler = new BasicHandler(name, priority);
            return handler;
        }

        @Override
        public String[] getAliases() {
            return basicAliases;
        }

        @Override
        public String getType() {
            return "basic";
        }

        @Override
        public String getPrimaryAlias() {
            return "basic";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }
}