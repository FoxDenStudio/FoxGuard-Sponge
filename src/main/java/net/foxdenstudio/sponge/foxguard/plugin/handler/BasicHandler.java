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
import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FGStorageManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Entry;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Operation;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.EverythingSet;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;
import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.*;

public class BasicHandler extends HandlerBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(INDEX_ALIASES, key) && !map.containsKey("index")) {
            map.put("index", value);
        } else if (isIn(COLOR_ALIASES, key) && !map.containsKey("color")) {
            map.put("color", value);
        } else if (isIn(DISPLAY_NAME_ALIASES, key) && !map.containsKey("displayname")) {
            map.put("displayname", value);
        }
        return true;
    };

    private final List<Group> groups;
    private final Map<Group, List<Entry>> groupPermissions;
    private final Group defaultGroup;
    private final List<Entry> defaultPermissions;

    private final Map<Group, Map<FlagBitSet, Tristate>> groupPermCache;
    private final Map<FlagBitSet, Tristate> defaultPermCache;
    private final Map<Set<Group>, Map<FlagBitSet, Tristate>> groupSetPermCache;
    private final Map<UUID, Set<Group>> userGroupCache;
    private final Map<UUID, Map<FlagBitSet, Tristate>> userPermCache;

    private PassiveSetting passiveSetting = PassiveSetting.PASSTHROUGH;
    private Group passiveGroup;
    private Map<FlagBitSet, Tristate> passiveGroupCacheRef;
    private final Map<FlagBitSet, Tristate> passivePermCache;

    public BasicHandler(String name, int priority) {
        this(name, true, priority,
                new ArrayList<>(),
                new HashMap<>(),
                new Group("default", EverythingSet.get(), TextColors.RED, "Default"),
                new ArrayList<>());
    }

    public BasicHandler(String name, boolean isEnabled, int priority,
                        List<Group> groups,
                        Map<Group, List<Entry>> groupPermissions,
                        Group defaultGroup,
                        List<Entry> defaultPermissions) {
        super(name, priority, isEnabled);
        this.groups = groups;
        this.defaultGroup = defaultGroup;

        this.groupPermissions = groupPermissions;
        this.defaultPermissions = defaultPermissions;

        this.groupPermCache = new CacheMap<>((k1, m1) -> {
            if (k1 instanceof Group) {
                List<Entry> entries = BasicHandler.this.groupPermissions.get(k1);
                Map<FlagBitSet, Tristate> map = new CacheMap<>((k2, m2) -> {
                    if (k2 instanceof FlagBitSet) {
                        FlagBitSet flags = (FlagBitSet) k2;
                        Tristate state = null;
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
        this.groupSetPermCache = new CacheMap<>((k1, m1) -> {
            if (k1 instanceof Set) {
                for (Object o : (Set) k1) {
                    if (!(o instanceof Group)) return null;
                }
                Set<Group> set = (Set<Group>) k1;
                List<Group> list = new ArrayList<>(set);
                Collections.sort(list, (g1, g2) -> this.groups.indexOf(g1) - this.groups.indexOf(g2));
                Map<FlagBitSet, Tristate> map = new CacheMap<>((k2, m2) -> {
                    if (k2 instanceof FlagBitSet) {
                        Tristate state = null;
                        FlagBitSet flags = (FlagBitSet) k2;
                        for (Group group : list) {
                            state = this.groupPermCache.get(group).get(flags);
                            if (state != null) break;
                        }
                        if (state == null) state = this.defaultPermCache.get(flags);
                        m2.put(flags, state);
                        return state;
                    } else return null;
                });
                m1.put(set, map);
                return map;
            } else return null;
        });
        this.userGroupCache = new CacheMap<>((k, m) -> {
            if (k instanceof UUID) {
                UUID user = ((UUID) k);
                Set<Group> userGroups = this.groups.stream().filter(group -> group.users.contains(user)).collect(GuavaCollectors.toImmutableSet());
                m.put(user, userGroups);
                return userGroups;
            } else return null;
        });
        this.userPermCache = new CacheMap<>((k, m) -> {
            if (k instanceof UUID) {
                UUID user = ((UUID) k);
                Set<Group> userGroups = this.userGroupCache.get(user);
                Map<FlagBitSet, Tristate> permCache;
                if (userGroups.size() == 0)
                    permCache = this.defaultPermCache;
                else {
                    permCache = this.groupSetPermCache.get(userGroups);
                }
                m.put(user, permCache);
                return permCache;
            } else return null;
        });
        this.passivePermCache = new CacheMap<>((k, m) -> {
            if (k instanceof FlagBitSet) {
                FlagBitSet flags = (FlagBitSet) k;
                Tristate state = Tristate.UNDEFINED;
                switch (passiveSetting) {
                    case ALLOW:
                        state = TRUE;
                        break;
                    case DENY:
                        state = FALSE;
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

    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify a command!"));

        if (isIn(GROUPS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an operation!"));

            switch (parse.args[1].toLowerCase()) {
                case "add": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify a group name!"));
                    if (name.equalsIgnoreCase("default"))
                        return ProcessResult.of(false, Text.of("Default group already exists and its name is reserved!"));
                    if (!isNameValid(parse.args[2]))
                        return ProcessResult.of(false, Text.of("Not a valid group name!"));
                    Optional<Group> groupOptional = createGroup(parse.args[2]);
                    if (!groupOptional.isPresent())
                        return ProcessResult.of(false, Text.of("Group already exists with this name!"));

                    Group group = groupOptional.get();
                    if (parse.flags.containsKey("color")) {
                        String colorString = parse.flags.get("color");
                        Optional<TextColor> colorOptional = FCPUtil.textColorFromHex(colorString);
                        if (!colorOptional.isPresent())
                            colorOptional = FCPUtil.textColorFromName(colorString);
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
                }
                case "remove": {
                    if (parse.args.length <= 2)
                        return ProcessResult.of(false, Text.of("Must specify a group to remove!"));
                    if (name.equalsIgnoreCase("default"))
                        return ProcessResult.of(false, Text.of("Default group cannot be removed!"));
                    Optional<Group> groupOptional = getGroup(parse.args[2]);
                    if (!groupOptional.isPresent())
                        return ProcessResult.of(false, Text.of("Not a valid group!"));
                    removeGroup(groupOptional.get());
                    return ProcessResult.of(true, Text.of("Successfully removed group!"));
                }
                case "modify": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify a group name!"));
                    Optional<Group> groupOptional = getGroup(parse.args[2]);
                    if (!groupOptional.isPresent())
                        return ProcessResult.of(false, Text.of("No group exists with this name!"));

                    Group group = groupOptional.get();
                    if (parse.flags.containsKey("color")) {
                        String colorString = parse.flags.get("color");
                        if (!colorString.isEmpty()) {
                            Optional<TextColor> colorOptional = FCPUtil.textColorFromHex(colorString);
                            if (!colorOptional.isPresent())
                                colorOptional = FCPUtil.textColorFromName(colorString);
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
                }
                case "rename": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify a group name!"));
                    if (name.equalsIgnoreCase("default"))
                        return ProcessResult.of(false, Text.of("Default group cannot be renamed!"));
                    Optional<Group> groupOptional = getGroup(parse.args[2]);
                    if (!groupOptional.isPresent())
                        return ProcessResult.of(false, Text.of("No group exists with this name!"));
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify a new group name!"));
                    if (!isNameValid(parse.args[3]))
                        return ProcessResult.of(false, Text.of("New group name is not valid!"));
                    renameGroup(groupOptional.get(), parse.args[3]);
                    return ProcessResult.of(true, Text.of("Successfully renamed group!"));
                }
                case "move": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify a group name!"));
                    if (name.equalsIgnoreCase("default"))
                        return ProcessResult.of(false, Text.of("Default group cannot be moved!"));
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
                                newIndex = FCCUtil.parseCoordinate(currentIndex, target) - 1;
                            } catch (NumberFormatException ignored) {
                                return ProcessResult.of(false, Text.of("Target index number is not formatted correctly!"));
                            }
                        } else {
                            return ProcessResult.of(false, Text.of("Not a valid target position!"));
                        }
                        moveGroup(group, newIndex);
                    }
                    return ProcessResult.of(true, Text.of("Successfully moved group!"));
                }
                default:
                    return ProcessResult.of(false, Text.of("Not a valid group operation!"));
            }
        } else if (isIn(USERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify a group!"));
            Optional<Group> groupOptional = getGroup(parse.args[1]);
            Group group;
            if (!groupOptional.isPresent()) {
                return ProcessResult.of(false, Text.of("No group exists with this name!"));
            } else {
                group = groupOptional.get();
            }
            if (parse.args.length < 3) return ProcessResult.of(false, Text.of("Must specify an operation!"));
            Operation op;
            switch (parse.args[2].toLowerCase()) {
                case "add":
                    op = Operation.ADD;
                    break;
                case "remove":
                    op = Operation.REMOVE;
                    break;
                case "set":
                    op = Operation.SET;
                    break;
                default:
                    return ProcessResult.of(false, Text.of("Not a valid operation!"));
            }
            if (parse.args.length < 4) {
                if (op != Operation.SET)
                    return ProcessResult.of(false, Text.of("Must specify a user or a group to add!"));
                else {
                    clearUsers(group);
                    return ProcessResult.of(true, Text.of("Successfully cleared group!"));
                }
            }
            Set<UUID> users = new HashSet<>();
            int successes = 0, failures = 0;
            String[] newArgs = Arrays.copyOfRange(parse.args, 3, parse.args.length);
            for (String s : newArgs) {
                try {
                    UUID uuid = UUID.fromString(s);
                    users.add(uuid);
                } catch (IllegalArgumentException e) {
                    if (s.startsWith("#")) {
                        s = s.substring(1);
                        Optional<Group> groupOpt = getGroup(s);
                        if (groupOpt.isPresent()) {
                            Group argGroup = groupOpt.get();
                            argGroup.users.stream()
                                    .filter(user -> !users.contains(user))
                                    .forEach(users::add);
                        } else if (newArgs.length == 1) {
                            return ProcessResult.of(false, Text.of("No group exists with this name!"));
                        } else {
                            failures++;
                        }
                    } else {
                        Optional<User> userOptional = FoxGuardMain.instance().getUserStorage().get(s);
                        if (userOptional.isPresent()) {
                            users.add(userOptional.get().getUniqueId());
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
                    for (UUID u : users) {
                        if (addUser(group, u)) {
                            successes++;
                        } else {
                            failures++;
                        }
                    }
                    break;
                case REMOVE:
                    for (UUID u : users) {
                        if (removeUser(group, u)) {
                            successes++;
                        } else {
                            failures++;
                        }
                    }
                    break;
                case SET:
                    clearUsers(group);
                    for (UUID u : users) {
                        if (addUser(group, u)) {
                            successes++;
                        } else {
                            failures++;
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
        } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify a group!"));
            Optional<Group> groupOptional = getGroup(parse.args[1]);
            Group group;
            if (!groupOptional.isPresent()) {
                return ProcessResult.of(false, Text.of("No group exists with the name \"" + parse.args[1] + "\"!"));
            } else {
                group = groupOptional.get();
            }
            if (parse.args.length < 3) return ProcessResult.of(false, Text.of("Must specify an operation!"));

            switch (parse.args[2].toLowerCase()) {
                case "add": {
                    Tristate state = null;
                    Set<Flag> flags = new HashSet<>();
                    boolean areFlagsSet = false;
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify flags and a tristate value!"));
                    for (int i = 3; i < parse.args.length; i++) {
                        String argument = parse.args[i];
                        if (argument.startsWith("=")) {
                            argument = argument.substring(1);
                            if (argument.isEmpty())
                                return ProcessResult.of(false, Text.of("Must supply a tristate value after \'=\'!"));
                            Tristate newState = tristateFrom(argument);
                            if (newState != null) {
                                state = newState;
                            } else {
                                return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid tristate value!"));
                            }
                        } else {
                            Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                            if (flagOptional.isPresent()) {
                                flags.add(flagOptional.get());
                                areFlagsSet = true;
                            } else {
                                return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                            }
                        }
                    }
                    if (!areFlagsSet) return ProcessResult.of(false, Text.of("Must specify flags!"));
                    if (state == null) return ProcessResult.of(false, Text.of("Must specify a tristate value!"));
                    List<Entry> permissions = getGroupPermissions(group);
                    Entry entry = new Entry(flags, state);

                    for (Entry existing : permissions) {
                        if (existing.set.equals(entry.set))
                            return ProcessResult.of(false, Text.of("Entry already exists with this flag set!"));
                    }

                    int index = 0;
                    if (parse.flags.containsKey("index")) {
                        String number = parse.flags.get("index");
                        if (!number.isEmpty()) {
                            try {
                                index = Integer.parseInt(number);
                                if (index < 0) index = 0;
                                else if (index > permissions.size()) index = permissions.size();
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    this.addFlagEntry(group, index, entry);
                    return ProcessResult.of(true, Text.of("Successfully added flag entry!"));
                }
                case "remove": {
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify flags or an index to remove!"));
                    List<Entry> permissions = getGroupPermissions(group);
                    try {
                        int index = Integer.parseInt(parse.args[3]);
                        if (index < 0) index = 0;
                        else if (index >= permissions.size()) index = permissions.size() - 1;
                        removeFlagEntry(group, index);
                        return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                    } catch (NumberFormatException ignored) {
                        Set<Flag> flags = new HashSet<>();
                        for (int i = 3; i < parse.args.length; i++) {
                            String argument = parse.args[i];
                            Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                            if (flagOptional.isPresent()) {
                                flags.add(flagOptional.get());
                            } else {
                                return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                            }
                        }
                        Entry entry = null;
                        for (Entry existing : permissions) {
                            if (existing.set.equals(flags)) {
                                entry = existing;
                                break;
                            }
                        }
                        if (entry == null) return ProcessResult.of(false, Text.of("No flag entry with these flags!"));
                        removeFlagEntry(group, permissions.indexOf(entry));
                        return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                    }
                }
                case "set": {
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify an index or flags and then a tristate value!"));
                    List<Entry> permissions = getGroupPermissions(group);
                    try {
                        int index = Integer.parseInt(parse.args[3]);
                        if (index < 0) index = 0;
                        else if (index >= permissions.size()) index = permissions.size() - 1;
                        if (parse.args.length < 5)
                            return ProcessResult.of(false, Text.of("Must specify a tristate value!"));
                        String tristateArg = parse.args[4];
                        if (tristateArg.startsWith("=")) tristateArg = tristateArg.substring(1);
                        if (isIn(CLEAR_ALIASES, tristateArg)) {
                            this.removeFlagEntry(group, index);
                            return ProcessResult.of(true, Text.of("Successfully cleared flag entry!"));
                        } else {
                            Tristate state = Aliases.tristateFrom(tristateArg);
                            if (state == null)
                                return ProcessResult.of(false, Text.of("\"" + tristateArg + "\" is not a valid tristate value!"));
                            this.setFlagEntry(group, index, state);
                            return ProcessResult.of(true, Text.of("Successfully set flag entry!"));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    Tristate state = null;
                    Set<Flag> flags = new HashSet<>();
                    boolean areFlagsSet = false;
                    boolean clear = false;
                    for (int i = 3; i < parse.args.length; i++) {
                        String argument = parse.args[i];
                        if (argument.startsWith("=")) {
                            argument = argument.substring(1);
                            if (argument.isEmpty())
                                return ProcessResult.of(false, Text.of("Must supply a tristate value after \'=\'!"));
                            if (isIn(CLEAR_ALIASES, argument)) {
                                clear = true;
                            } else {
                                Tristate newState = tristateFrom(argument);
                                if (newState != null) {
                                    state = newState;
                                    clear = false;
                                } else {
                                    return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid tristate value!"));
                                }
                            }
                        } else {
                            Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                            if (flagOptional.isPresent()) {
                                flags.add(flagOptional.get());
                                areFlagsSet = true;
                            } else {
                                return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                            }
                        }
                    }
                    if (!areFlagsSet) return ProcessResult.of(false, Text.of("Must specify flags!"));
                    if (state == null && !clear)
                        return ProcessResult.of(false, Text.of("Must specify a tristate value!"));
                    Entry entry = new Entry(flags, state);

                    Entry original = null;
                    for (Entry existing : permissions) {
                        if (existing.set.equals(entry.set)) {
                            original = existing;
                            break;
                        }
                    }

                    if (clear) {
                        if (original != null) {
                            this.removeFlagEntry(group, permissions.indexOf(entry));
                            return ProcessResult.of(true, Text.of("Successfully cleared flag entry!"));
                        } else {
                            return ProcessResult.of(false, Text.of("No entry exists with these flags to be cleared!"));
                        }
                    } else {
                        if (original != null) permissions.remove(original);
                        int index = 0;
                        if (parse.flags.containsKey("index")) {
                            String number = parse.flags.get("index");
                            if (!number.isEmpty()) {
                                try {
                                    index = Integer.parseInt(number);
                                    if (index < 0) index = 0;
                                    else if (index > permissions.size()) index = permissions.size();
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        this.setFlagEntry(group, index, entry);
                        return ProcessResult.of(true, Text.of("Successfully set flag entry!"));
                    }
                }
                case "move": {
                    List<Entry> permissions = getGroupPermissions(group);
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify flags or an index to move!"));
                    try {
                        int from = Integer.parseInt(parse.args[3]);
                        if (from < 0) from = 0;
                        else if (from >= permissions.size()) from = permissions.size() - 1;
                        if (parse.args.length < 5)
                            return ProcessResult.of(false, Text.of("Must specify a target index to move to!"));
                        int to = FCCUtil.parseCoordinate(from, parse.args[4]);
                        if (to < 0) to = 0;
                        else if (to >= permissions.size()) to = permissions.size() - 1;
                        moveFlagEntry(group, from, to);
                        return ProcessResult.of(true, Text.of("Successfully moved flag entry!"));
                    } catch (NumberFormatException ignored) {
                        Set<Flag> flags = new HashSet<>();
                        int index = 0;
                        boolean set = false;
                        boolean relative = false;
                        for (int i = 3; i < parse.args.length; i++) {
                            String argument = parse.args[i];
                            Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                            if (flagOptional.isPresent()) {
                                flags.add(flagOptional.get());
                                continue;
                            }
                            try {
                                String arg = argument;
                                if (arg.startsWith("~")) {
                                    arg = arg.substring(1);
                                    relative = true;
                                }
                                index = Integer.parseInt(arg);
                                set = true;
                                continue;
                            } catch (NumberFormatException ignored2) {
                            }
                            return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                        }
                        if (!set) return ProcessResult.of(false, Text.of("Must specify a target index!"));
                        Entry entry = null;
                        for (Entry existing : permissions) {
                            if (existing.set.equals(flags)) {
                                entry = existing;
                                break;
                            }
                        }

                        if (entry == null) return ProcessResult.of(false, Text.of("No flag entry with these flags!"));
                        if (relative) index += permissions.indexOf(entry);
                        moveFlagEntry(group, entry.set, index);
                    }
//                    removeFlagEntry(group, permissions.indexOf(entry));
                    return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                }
                default:
                    return ProcessResult.of(false, Text.of("Not a valid flag operation!"));
            }
        } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an option!"));

            if (isIn(TRUE_ALIASES, parse.args[1])) {
                this.setPassiveSetting(PassiveSetting.ALLOW);
            } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                this.setPassiveSetting(PassiveSetting.DENY);
            } else if (isIn(PASSTHROUGH_ALIASES, parse.args[1])) {
                this.passiveSetting = PassiveSetting.PASSTHROUGH;
            } else if (isIn(GROUPS_ALIASES, parse.args[1])) {
                if (parse.args.length < 3)
                    return ProcessResult.of(false, Text.of("Must specify a group name!"));
                Optional<Group> groupOptional = getGroup(parse.args[2]);
                if (!groupOptional.isPresent())
                    return ProcessResult.of(false, Text.of("No group exists with this name!"));

                this.setPassiveSetting(PassiveSetting.GROUP, groupOptional.get());
            } else if (isIn(DEFAULT_GROUP_ALIASES, parse.args[1])) {
                this.setPassiveSetting(PassiveSetting.DEFAULT);
            } else {
                return ProcessResult.of(false, Text.of("Not a valid option!"));
            }
            return ProcessResult.of(true, Text.of("Successfully set passive option!"));
        } else {
            return ProcessResult.of(false, Text.of("Not a valid basic handler command!"));
        }
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("groups", "users", "flags", "passive").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "modify", "rename", "move").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(USERS_ALIASES, parse.args[0])) {
                    return this.groups.stream()
                            .map(Group::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                    List<String> list = this.groups.stream()
                            .map(Group::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(Collectors.toList());
                    list.add("default");
                    return ImmutableList.copyOf(list);
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("allow", "deny", "pass", "group", "default").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    switch (parse.args[1].toLowerCase()) {
                        case "add": {
                            if (!parse.current.token.isEmpty()) {
                                Optional<Group> groupOptional = getGroup(parse.current.token);
                                if (groupOptional.isPresent()) {
                                    source.sendMessage(Text.of(TextColors.RED, "Name is already taken!"));
                                } else {
                                    source.sendMessage(Text.of(TextColors.GREEN, "Name is available!"));
                                }
                            }
                            return ImmutableList.of();
                        }
                        case "modify": {
                            List<String> list = this.groups.stream()
                                    .map(Group::getName)
                                    .collect(Collectors.toList());
                            list.add("default");
                            return list.stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        }
                        case "remove":
                        case "rename":
                        case "move": {
                            return this.groups.stream()
                                    .map(Group::getName)
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        }
                    }
                } else if (isIn(USERS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                    return ImmutableList.of("add", "remove", "set", "move").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    if (isIn(GROUPS_ALIASES, parse.args[1])) {
                        return this.groups.stream()
                                .map(Group::getName)
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            } else if (parse.current.index >= 3) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    if (parse.current.index == 3) {
                        if (parse.args[1].equalsIgnoreCase("rename")) {
                            if (!parse.current.token.isEmpty()) {
                                Optional<Group> groupOptional = getGroup(parse.current.token);
                                if (groupOptional.isPresent()) {
                                    source.sendMessage(Text.of(TextColors.RED, "Name is already taken!"));
                                } else {
                                    source.sendMessage(Text.of(TextColors.GREEN, "Name is available!"));
                                }
                            }
                        }
                    }
                } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                    switch (parse.args[2].toLowerCase()) {
                        case "add": {
                            if (parse.current.token.startsWith("=")) {
                                return ImmutableList.of("=allow", "=deny", "=pass").stream()
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            } else {
                                String[] flagArgs = Arrays.copyOfRange(parse.args, 3, parse.args.length);
                                return FlagRegistry.getInstance().getFlagList().stream()
                                        .map(Flag::getName)
                                        .filter(arg -> !isIn(flagArgs, arg))
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            }
                        }
                        case "set": {
                            if (parse.current.index == 3) {
                                if (parse.current.token.startsWith("=")) {
                                    return ImmutableList.of("=allow", "=deny", "=pass", "=clear").stream()
                                            .filter(new StartsWithPredicate(parse.current.token))
                                            .map(args -> parse.current.prefix + args)
                                            .collect(GuavaCollectors.toImmutableList());
                                } else {
                                    return FlagRegistry.getInstance().getFlagList().stream()
                                            .map(Flag::getName)
                                            .filter(new StartsWithPredicate(parse.current.token))
                                            .map(args -> parse.current.prefix + args)
                                            .collect(GuavaCollectors.toImmutableList());
                                }
                            } else if (parse.current.index == 4) try {
                                Integer.parseInt(parse.args[3]);
                                return ImmutableList.of("allow", "deny", "pass", "clear").stream()
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            } catch (NumberFormatException ignored) {
                            }
                            if (parse.current.token.startsWith("=")) {
                                return ImmutableList.of("=allow", "=deny", "=pass", "=clear").stream()
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            } else {
                                String[] flagArgs = Arrays.copyOfRange(parse.args, 3, parse.args.length);
                                return FlagRegistry.getInstance().getFlagList().stream()
                                        .map(Flag::getName)
                                        .filter(arg -> !isIn(flagArgs, arg))
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            }
                        }
                        case "move":
                        case "remove": {
                            String[] flagArgs = Arrays.copyOfRange(parse.args, 3, parse.args.length);
                            return FlagRegistry.getInstance().getFlagList().stream()
                                    .map(Flag::getName)
                                    .filter(arg -> !isIn(flagArgs, arg))
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        }
                    }
                } else if (isIn(USERS_ALIASES, parse.args[0])) {
                    Optional<Group> groupOptional = getGroup(parse.args[1]);
                    if (groupOptional.isPresent()) {
                        Group group = groupOptional.get();
                        String[] entries = Arrays.copyOfRange(parse.args, 3, parse.args.length);
                        Operation op;
                        switch (parse.args[2].toLowerCase()) {
                            case "add":
                                op = Operation.ADD;
                                break;
                            case "remove":
                                op = Operation.REMOVE;
                                break;
                            case "set":
                                op = Operation.SET;
                                break;
                            default:
                                op = null;
                        }
                        return ImmutableList.<String>builder()
                                .addAll(Sponge.getServer().getOnlinePlayers().stream()
                                        .filter(player -> Operation.userFilter(op, group.users.contains(player.getUniqueId())))
                                        .map(Player::getName)
                                        .filter(entry -> !isIn(entries, entry))
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .collect(Collectors.toList())
                                )
                                .addAll(this.groups.stream()
                                        .filter(group1 -> {
                                            for (UUID uuid : group1.users) {
                                                if (Operation.userFilter(op, group.users.contains(uuid))) return true;
                                            }
                                            return false;
                                        })
                                        .map(Group::getName)
                                        .filter(entry -> !isIn(entries, entry))
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .map(name -> "#" + name)
                                        .collect(Collectors.toList())
                                )
                                .build().stream()
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY)) {
            if (isIn(GROUPS_ALIASES, parse.args[0])) {
                return ImmutableList.of("index", "color", "displayname").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                ImmutableList.of("index").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(GROUPS_ALIASES, parse.args[0])) {
                if (isIn(COLOR_ALIASES, parse.current.key)) {
                    return Arrays.stream(FCCUtil.colorNames)
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
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        if (user == null) return EventResult.of(this.passivePermCache.get(flags));
        else return EventResult.of(this.userPermCache.get(user.getUniqueId()).get(flags));
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
        UserStorageService userStorageService = FoxGuardMain.instance().getUserStorage();
        Text.Builder builder = Text.builder();
        Text.Builder passiveBuilder = Text.builder()
                .append(Text.of(TextColors.AQUA, "Passive: "))
                .append(Text.of(TextColors.RESET, this.passiveSetting.toString()));
        if (this.passiveSetting == PassiveSetting.GROUP)
            passiveBuilder.append(Text.of(passiveGroup.color, passiveGroup.displayName));
        passiveBuilder
                .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " passive "))
                .onHover(TextActions.showText(Text.of("Click to change the passive config")));
        builder.append(passiveBuilder.build());
        builder.append(Text.NEW_LINE);
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.getName() + " group add "),
                TextActions.showText(Text.of("Click to add a group")),
                "----- Group Members -----\n"));
        for (Group group : groups) {
            builder.append(Text.of(group.color,
                    TextActions.suggestCommand("/foxguard md h " + this.getName() + " users " + group.name + " add "),
                    TextActions.showText(Text.of("Click to add player(s) to \"", group.color, group.displayName, TextColors.RESET, "\"" + (group.name.equals(group.displayName) ? "" : " (" + group.name + ")"))),
                    group.displayName,
                    TextColors.RESET, ": "));
            Set<User> online = new HashSet<>();
            Set<UUID> offline = new HashSet<>();
            for (UUID uuid : group.users) {
                Optional<User> userOptional = userStorageService.get(uuid);
                if (userOptional.isPresent()) {
                    online.add(userOptional.get());
                } else {
                    offline.add(uuid);
                }
            }
            online.stream()
                    .sorted((u1, u2) -> u1.getName().compareTo(u2.getName()))
                    .forEach(user -> {
                        TextColor color = TextColors.WHITE;
                        if (source instanceof Player && ((Player) source).getUniqueId().equals(user.getUniqueId()))
                            color = TextColors.YELLOW;
                        builder.append(Text.of(color,
                                TextActions.suggestCommand("/foxguard md h " + this.getName() + " users " + group.name + " remove " + user.getName()),
                                TextActions.showText(Text.of("Click to remove player \"" + user.getName() + "\" from \"", group.color, group.displayName, TextColors.RESET, "\"" + (group.name.equals(group.displayName) ? "" : " (" + group.name + ")"))),
                                user.getName())).append(Text.of(" "));
                    });
            offline.stream()
                    .sorted((u1, u2) -> u1.toString().compareTo(u2.toString()))
                    .forEach(uuid -> builder.append(Text.of(TextColors.RESET,
                            TextActions.suggestCommand("/foxguard md h " + this.getName() + " users " + group.name + " remove " + uuid.toString()),
                            TextActions.showText(Text.of("Click to remove player \"" + uuid.toString() + "\" from \"", group.color, group.displayName, TextColors.RESET, "\"" + (group.name.equals(group.displayName) ? "" : " (" + group.name + ")"))),
                            uuid.toString())).append(Text.of(" ")));

            builder.append(Text.NEW_LINE);
        }
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.getName() + " groups add "),
                TextActions.showText(Text.of("Click to add a group")),
                "----- Group Flags -----\n"));
        for (Group group : groups) {
            builder.append(Text.of(group.color,
                    TextActions.suggestCommand("/foxguard md h " + this.name + " flags " + group.name + " add "),
                    TextActions.showText(Text.of("Click to add a flag entry")),
                    group.displayName + ":\n"));
            int index = 0;
            for (Entry entry : this.groupPermissions.get(group)) {
                StringBuilder stringBuilder = new StringBuilder();
                entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
                Text.Builder entryBuilder = Text.builder();
                entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "))
                        .append(FGUtil.readableTristateText(entry.state))
                        .onHover(TextActions.showText(Text.of("Click to change this flag entry")))
                        .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " flags " + group.name + " set " + (index++) + " "));
                builder.append(entryBuilder.build()).append(Text.NEW_LINE);
            }
        }
        builder.append(Text.of(this.defaultGroup.color,
                TextActions.suggestCommand("/foxguard md h " + this.name + " flags default add "),
                TextActions.showText(Text.of("Click to add a flag entry")),
                this.defaultGroup.displayName + ":"));
        int index = 0;
        for (Entry entry : this.defaultPermissions) {
            StringBuilder stringBuilder = new StringBuilder();
            entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
            Text.Builder entryBuilder = Text.builder();
            entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "))
                    .append(FGUtil.readableTristateText(entry.state))
                    .onHover(TextActions.showText(Text.of("Click to change this flag entry")))
                    .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " flags default set " + (index++) + " "));
            builder.append(Text.NEW_LINE).append(entryBuilder.build());
        }
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        FGStorageManager storageManager = FGStorageManager.getInstance();
        UserStorageService userStorageService = FoxGuardMain.instance().getUserStorage();
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("groups.foxdb").normalize().toString()).make()) {
            List<String> groupNames = flagMapDB.indexTreeList("names", Serializer.STRING).createOrOpen();
            groupNames.clear();
            groupNames.addAll(this.groups.stream().map(group -> group.name).collect(Collectors.toList()));
        }
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.foxdb").normalize().toString()).make()) {
            for (Group group : this.groups) {
                List<String> stringEntries = flagMapDB.indexTreeList(group.name, Serializer.STRING).createOrOpen();
                stringEntries.clear();
                stringEntries.addAll(this.groupPermissions.get(group).stream().map(Entry::serialize).collect(Collectors.toList()));
            }
            List<String> stringEntries = flagMapDB.indexTreeList("default", Serializer.STRING).createOrOpen();
            stringEntries.clear();
            stringEntries.addAll(this.defaultPermissions.stream().map(Entry::serialize).collect(Collectors.toList()));
        }
        Path groupsDirectory = directory.resolve("groups");
        storageManager.constructDirectory(groupsDirectory);
        for (Group group : this.groups) {
            Path groupFile = groupsDirectory.resolve(group.name + ".cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(groupFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(groupFile, loader);
            List<Map<String, Object>> members = new ArrayList<>();
            for (UUID uuid : group.users) {
                Optional<User> userOptional = userStorageService.get(uuid);
                Map<String, Object> map = new HashMap<>();
                if (userOptional.isPresent()) map.put("username", userOptional.get().getName());
                map.put("uuid", uuid.toString());
                members.add(map);
            }
            root.getNode("members").setValue(members);
            root.getNode("displayname").setValue(group.displayName);
            root.getNode("color").setValue(group.color.getName());
            try {
                loader.save(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        {
            Path basicFile = directory.resolve("basic.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(basicFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(basicFile, loader);
            CommentedConfigurationNode defaultNode = root.getNode("default");
            defaultNode.getNode("displayname").setValue(this.defaultGroup.displayName);
            defaultNode.getNode("color").setValue(this.defaultGroup.color.getName());
            CommentedConfigurationNode passiveNode = root.getNode("passive");
            passiveNode.getNode("setting").setValue(this.passiveSetting.name());
            passiveNode.getNode("group").setValue(this.passiveGroup == null ? "" : this.passiveGroup.name);
            try {
                loader.save(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        if (name.equalsIgnoreCase("default")) return Optional.of(defaultGroup);
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return Optional.of(g);
        }
        return Optional.empty();
    }

    public Optional<Group> getOrCreateGroup(String name) {
        if (name.equalsIgnoreCase("default")) return Optional.of(defaultGroup);
        if (!isNameValid(name.toLowerCase())) return Optional.empty();
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return Optional.of(g);
        }
        Group group = new Group(name);
        this.groups.add(group);
        this.groupPermissions.put(group, new ArrayList<>());
        return Optional.of(group);
    }

    public Group getDefaultGroup() {
        return defaultGroup;
    }

    public boolean groupExists(String name) {
        if (name.equalsIgnoreCase("default")) return true;
        for (Group g : this.groups) {
            if (g.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public boolean removeGroup(Group group) {
        if (this.groups.contains(group)) {
            this.groupPermissions.remove(group);
            this.groupPermCache.remove(group);
            group.users.forEach(this.userGroupCache::remove);
            group.users.forEach(this.userPermCache::remove);
            Set<Set<Group>> groupSuperSet = new HashSet<>();
            for (Map.Entry<Set<Group>, Map<FlagBitSet, Tristate>> entry : this.groupSetPermCache.entrySet()) {
                Set<Group> key = entry.getKey();
                if (key.contains(group)) groupSuperSet.add(key);
            }
            groupSuperSet.forEach(this.groupSetPermCache::remove);
            this.groups.remove(group);
            if (group.equals(passiveGroup)) this.setPassiveSetting(PassiveSetting.PASSTHROUGH);
            return true;
        }
        return false;
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
        if (group == defaultGroup) return false;
        if (this.groups.contains(group)) {
            this.groups.remove(group);
            if (index < 0) index = 0;
            if (index > this.groups.size()) index = this.groups.size();
            this.groups.add(index, group);
            group.users.forEach(this.userPermCache::remove);
            Set<Set<Group>> groupSuperSet = new HashSet<>();
            for (Map.Entry<Set<Group>, Map<FlagBitSet, Tristate>> entry : this.groupSetPermCache.entrySet()) {
                Set<Group> key = entry.getKey();
                if (key.contains(group)) groupSuperSet.add(key);
            }
            groupSuperSet.forEach(this.groupSetPermCache::remove);
            return true;
        } else return false;
    }

    public boolean renameGroup(Group group, String newName) {
        if (group == defaultGroup) return false;
        if (this.groups.contains(group)) {
            if (groupExists(newName)) return false;
            group.name = newName.toLowerCase();
            return true;
        } else return false;
    }

    public boolean addUser(Group group, User user) {
        return this.addUser(group, user.getUniqueId());
    }

    public boolean addUser(Group group, UUID user) {
        if (group.users.contains(user)) {
            return false;
        } else {
            group.users.add(user);
            this.userPermCache.remove(user);
            this.userGroupCache.remove(user);
            return true;
        }
    }

    public boolean removeUser(Group group, User user) {
        return this.removeUser(group, user.getUniqueId());
    }

    public boolean removeUser(Group group, UUID user) {
        if (!group.users.contains(user)) {
            return false;
        } else if (group.users.remove(user)) {
            this.userGroupCache.remove(user);
            this.userPermCache.remove(user);
            return true;
        } else return false;
    }

    public void clearUsers(Group group) {
        for (UUID user : group.users) {
            this.userGroupCache.remove(user);
            this.userPermCache.remove(user);
        }
        group.users.clear();
    }

    public boolean addFlagEntry(Group group, Entry entry) {
        return addFlagEntry(group, 0, entry);
    }

    public boolean addFlagEntry(Group group, int index, Entry entry) {
        List<Entry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index > groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + groupEntries.size());
        for (Entry groupEntry : groupEntries) {
            if (groupEntry.set.equals(entry.set)) return false;
        }
        groupEntries.add(index, entry);
        clearFlagCacheForGroup(group);
        return true;
    }

    public void setFlagEntry(Group group, Entry entry) {
        List<Entry> groupEntries = getGroupPermissions(group);
        for (Entry groupEntry : groupEntries) {
            if (groupEntry.set.equals(entry.set)) {
                groupEntry.state = entry.state;
                return;
            }
        }
        groupEntries.add(entry);
        clearFlagCacheForGroup(group);
    }

    public void setFlagEntry(Group group, int index, Entry entry) {
        List<Entry> groupEntries = getGroupPermissions(group);
        Entry original = null;
        for (Entry groupEntry : groupEntries) {
            if (groupEntry.set.equals(entry.set)) {
                original = groupEntry;
                break;
            }
        }
        if (original != null) groupEntries.remove(original);
        groupEntries.add(index, entry);
        clearFlagCacheForGroup(group);
    }

    public void setFlagEntry(Group group, int index, Tristate state) {
        List<Entry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index >= groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (groupEntries.size() - 1));
        Entry entry = groupEntries.get(index);
        entry.state = state;
        clearFlagCacheForGroup(group);
    }

    public boolean removeFlagEntry(Group group, Set<Flag> flags) {
        List<Entry> groupEntries = getGroupPermissions(group);
        Entry toRemove = null;
        for (Entry groupEntry : groupEntries) {
            if (groupEntry.set.equals(flags)) {
                toRemove = groupEntry;
            }
        }
        if (toRemove == null) return false;
        groupEntries.remove(toRemove);
        clearFlagCacheForGroup(group);
        return true;
    }

    public void removeFlagEntry(Group group, int index) {
        List<Entry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index >= groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (groupEntries.size() - 1));
        groupEntries.remove(index);
        clearFlagCacheForGroup(group);
    }

    public boolean moveFlagEntry(Group group, Set<Flag> flags, int destination) {
        List<Entry> groupEntries = getGroupPermissions(group);
        if (destination < 0 || destination >= groupEntries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (groupEntries.size() - 1));
        Entry toMove = null;
        for (Entry groupEntry : groupEntries) {
            if (groupEntry.set.equals(flags)) {
                toMove = groupEntry;
            }
        }
        if (toMove == null) return false;
        groupEntries.remove(toMove);
        clearFlagCacheForGroup(group);
        return true;
    }

    public void moveFlagEntry(Group group, int source, int destination) {
        List<Entry> groupEntries = getGroupPermissions(group);
        if (source < 0 || source >= groupEntries.size())
            throw new IndexOutOfBoundsException("Source index out of bounds: " + source + " Range: 0-" + (groupEntries.size() - 1));
        if (destination < 0 || destination >= groupEntries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (groupEntries.size() - 1));
        Entry entry = groupEntries.remove(source);
        groupEntries.add(destination, entry);
        clearFlagCacheForGroup(group);
    }

    public PassiveSetting getPassiveSetting() {
        return passiveSetting;
    }

    public void setPassiveSetting(PassiveSetting passiveSetting) {
        setPassiveSetting(passiveSetting, null);
    }

    public void setPassiveSetting(PassiveSetting passiveSetting, Group group) {
        if (passiveSetting == null) return;
        if (passiveSetting != PassiveSetting.GROUP) {
            this.passiveSetting = passiveSetting;
            this.passiveGroup = null;
            this.passivePermCache.clear();
        } else if (group != null) {
            this.passiveSetting = PassiveSetting.GROUP;
            if (group == this.defaultGroup) {
                this.passiveSetting = PassiveSetting.DEFAULT;
                this.passiveGroup = null;
            } else {
                this.passiveGroup = group;
                this.passiveGroupCacheRef = this.groupPermCache.get(group);
            }
            this.passivePermCache.clear();
        }
    }

    private void clearFlagCacheForGroup(Group group) {
        if (group == defaultGroup) {
            this.defaultPermCache.clear();
            this.userPermCache.clear();
            this.groupSetPermCache.clear();
            if (this.passiveSetting == PassiveSetting.DEFAULT) this.passivePermCache.clear();
        } else {
            this.groupPermCache.get(group).clear();
            group.users.forEach(this.userPermCache::remove);
            Set<Set<Group>> groupSuperSet = new HashSet<>();
            for (Map.Entry<Set<Group>, Map<FlagBitSet, Tristate>> cacheEntry : this.groupSetPermCache.entrySet()) {
                Set<Group> key = cacheEntry.getKey();
                if (key.contains(group)) groupSuperSet.add(key);
            }
            groupSuperSet.forEach(this.groupSetPermCache::remove);
            if (this.passiveSetting == PassiveSetting.GROUP && this.passiveGroup == group)
                this.passivePermCache.clear();
        }

    }

    private List<Entry> getGroupPermissions(Group group) {
        if (group == this.defaultGroup) return this.defaultPermissions;
        else return this.groupPermissions.get(group);
    }

    public static boolean isNameValid(String name) {
        if (name.matches("^.*[ :\\.=;\"\'\\\\/\\{\\}\\(\\)\\[\\]<>#@\\|\\?\\*].*$")) return false;
        if (name.equalsIgnoreCase("default")) return false;
        for (String s : FGStorageManager.FS_ILLEGAL_NAMES) {
            if (name.equalsIgnoreCase(s)) return false;
        }
        return true;
    }

    public enum PassiveSetting {
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

    public static class Group {
        private String name;
        private String displayName;
        private TextColor color;
        private final Set<UUID> users;

        private Group(String name) {
            this(name, new HashSet<>());
        }

        private Group(String name, Set<UUID> users) {
            this(name, users, TextColors.WHITE, name);
        }

        private Group(String name, Set<UUID> users, TextColor color, String displayName) {
            this.name = name.toLowerCase();
            this.displayName = displayName;
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

        public Set<UUID> getUsers() {
            return ImmutableSet.copyOf(users);
        }
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] ALIASES = {"basic", "base"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
            BasicHandler handler = new BasicHandler(name, priority);
            if (parse.args.length > 0) {
                if (parse.args[0].equalsIgnoreCase("bare")) {
                    return handler;
                } else if (parse.args[0].equalsIgnoreCase("skeleton")) {
                    Group owners = handler.createGroup("owners").get();
                    owners.displayName = "Owners";
                    owners.color = TextColors.GOLD;

                    Group members = handler.createGroup("members").get();
                    members.displayName = "Members";
                    members.color = TextColors.GREEN;

                    return handler;
                } else if (parse.args[0].equalsIgnoreCase("default")) {
                    Group owners = handler.createGroup("owners").get();
                    owners.displayName = "Owners";
                    owners.color = TextColors.GOLD;
                    handler.addFlagEntry(owners, new Entry(ImmutableSet.of(DEBUFF), TRUE));
                    if (source instanceof Player) owners.users.add(((Player) source).getUniqueId());

                    Group members = handler.createGroup("members").get();
                    members.displayName = "Members";
                    members.color = TextColors.GREEN;

                    handler.setPassiveSetting(PassiveSetting.DEFAULT);

                    return handler;
                } else if (parse.args[0].equalsIgnoreCase("easy")) {
                    Group owners = handler.createGroup("owners").get();
                    owners.displayName = "Owners";
                    owners.color = TextColors.GOLD;
                    handler.addFlagEntry(owners, new Entry(ImmutableSet.of(DEBUFF), TRUE));
                    if (source instanceof Player) owners.users.add(((Player) source).getUniqueId());

                    Group members = handler.createGroup("members").get();
                    members.displayName = "Members";
                    members.color = TextColors.GREEN;

                    Group passive = handler.createGroup("passive").get();
                    passive.displayName = "Passive";
                    passive.color = TextColors.AQUA;
                    handler.setPassiveSetting(PassiveSetting.GROUP, passive);

                    return handler;
                } else if (parse.args[0].equalsIgnoreCase("plugandplay")) {
                    Group owners = handler.createGroup("owners").get();
                    owners.displayName = "Owners";
                    owners.color = TextColors.GOLD;
                    handler.addFlagEntry(owners, new Entry(ImmutableSet.of(DEBUFF), TRUE));
                    if (source instanceof Player) owners.users.add(((Player) source).getUniqueId());

                    Group members = handler.createGroup("members").get();
                    members.displayName = "Members";
                    members.color = TextColors.GREEN;
                    handler.addFlagEntry(members, new Entry(ImmutableSet.of(BLOCK), UNDEFINED));

                    Group passive = handler.createGroup("passive").get();
                    passive.displayName = "Passive";
                    passive.color = TextColors.AQUA;
                    handler.addFlagEntry(passive, new Entry(ImmutableSet.of(SPAWN, HOSTILE), FALSE));
                    handler.setPassiveSetting(PassiveSetting.GROUP, passive);

                    Group defaultG = handler.getDefaultGroup();
                    handler.addFlagEntry(defaultG, new Entry(ImmutableSet.of(BLOCK, CHANGE), FALSE));
                    handler.addFlagEntry(defaultG, new Entry(ImmutableSet.of(DAMAGE, ENTITY), FALSE));
                    handler.addFlagEntry(defaultG, new Entry(ImmutableSet.of(DAMAGE, LIVING), UNDEFINED));
                    handler.addFlagEntry(defaultG, new Entry(ImmutableSet.of(DAMAGE, PLAYER), FALSE));

                    return handler;
                } else throw new CommandException(Text.of("\"" + parse.args[0] + "\" is not a valid template!"));
            } else
                throw new CommandException(Text.of("Must specify a starting template! (\"easy\" or \"plugandplay\" are the recommended)"));
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            FGStorageManager storageManager = FGStorageManager.getInstance();
            List<String> groupNames = new ArrayList<>();
            try (DB flagMapDB = DBMaker.fileDB(directory.resolve("groups.foxdb").normalize().toString()).make()) {
                groupNames.addAll(flagMapDB.indexTreeList("names", Serializer.STRING).createOrOpen());
            }
            Path groupsDirectory = directory.resolve("groups");
            storageManager.constructDirectory(groupsDirectory);
            List<Group> groups = new ArrayList<>();
            for (String groupName : groupNames) {
                Path groupFile = groupsDirectory.resolve(groupName + ".cfg");
                ConfigurationLoader<CommentedConfigurationNode> loader =
                        HoconConfigurationLoader.builder().setPath(groupFile).build();
                CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(groupFile, loader);
                List<Optional<UUID>> optionalMemberUUIDsList = root.getNode("members").getList(o -> {
                    if (o instanceof HashMap) {
                        HashMap map = (HashMap) o;
                        return Optional.of(UUID.fromString((String) map.get("uuid")));
                    } else return Optional.empty();
                });
                Set<UUID> members = optionalMemberUUIDsList.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
                String displayName = root.getNode("displayname").getString(groupName);
                TextColor color = Sponge.getRegistry().getType(TextColor.class, root.getNode("color").getString("white")).orElse(TextColors.WHITE);
                groups.add(new Group(groupName, members, color, displayName));
            }
            Map<Group, List<Entry>> groupPermissions = new HashMap<>();
            List<Entry> defaultPermissions;
            try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.foxdb").normalize().toString()).make()) {
                for (Group group : groups) {
                    List<String> stringEntries = flagMapDB.indexTreeList(group.name, Serializer.STRING).createOrOpen();
                    groupPermissions.put(group, stringEntries.stream()
                            .map(Entry::deserialize)
                            .filter(entry -> !entry.set.isEmpty())
                            .distinct()
                            .collect(Collectors.toList()));
                }
                List<String> stringEntries = flagMapDB.indexTreeList("default", Serializer.STRING).createOrOpen();
                defaultPermissions = stringEntries.stream()
                        .map(Entry::deserialize)
                        .filter(entry -> !entry.set.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            }

            Path basicFile = directory.resolve("basic.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(basicFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(basicFile, loader);
            CommentedConfigurationNode defaultNode = root.getNode("default");
            String defaultDisplayName = defaultNode.getNode("displayname").getString("Default");
            TextColor defaultColor = Sponge.getRegistry().getType(TextColor.class, defaultNode.getNode("color").getString("red")).orElse(TextColors.RED);

            BasicHandler handler = new BasicHandler(name, isEnabled, priority,
                    groups,
                    groupPermissions,
                    new Group("default", EverythingSet.get(), defaultColor, defaultDisplayName),
                    defaultPermissions);

            CommentedConfigurationNode passiveNode = root.getNode("passive");
            String passiveSettingString = passiveNode.getNode("setting").getString("PASSTHROUGH");
            PassiveSetting passiveSetting;
            try {
                passiveSetting = PassiveSetting.valueOf(passiveSettingString);
            } catch (IllegalArgumentException ignored) {
                passiveSetting = PassiveSetting.PASSTHROUGH;
            }
            if (passiveSetting == PassiveSetting.GROUP) {
                Optional<Group> groupOptional = handler.getGroup(passiveNode.getNode("group").getString(""));
                if (groupOptional.isPresent()) {
                    handler.setPassiveSetting(PassiveSetting.GROUP, groupOptional.get());
                } else {
                    handler.setPassiveSetting(PassiveSetting.PASSTHROUGH);
                }
            } else {
                handler.setPassiveSetting(passiveSetting);
            }

            return handler;
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "basic";
        }

        @Override
        public String getPrimaryAlias() {
            return getType();
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .excludeCurrent(true)
                    .autoCloseQuotes(true)
                    .parse();
            if (parse.current.type == AdvCmdParser.CurrentElement.ElementType.ARGUMENT &&
                    parse.current.index == 0) {
                return ImmutableList.of("bare", "skeleton", "default", "easy", "plugandplay").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
            return ImmutableList.of();
        }
    }
}
