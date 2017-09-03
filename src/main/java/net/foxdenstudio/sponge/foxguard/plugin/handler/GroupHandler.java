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
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FGStorageManagerOld;
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Operation;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.TristateEntry;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public class GroupHandler extends HandlerBase {

    private static final String[] PERMISSION_ALIASES = {"permission", "perm", "perms", "p"};

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(INDEX_ALIASES, key) && !map.containsKey("index")) {
            map.put("index", value);
        } else if (isIn(COLOR_ALIASES, key) && !map.containsKey("color")) {
            map.put("color", value);
        } else if (isIn(DISPLAY_NAME_ALIASES, key) && !map.containsKey("displayname")) {
            map.put("displayname", value);
        } else if (isIn(PERMISSION_ALIASES, key) && !map.containsKey("permission")) {
            map.put("permission", value);
        }
        return true;
    };

    private final List<Group> groups;
    private final Map<Group, List<TristateEntry>> groupPermissions;
    private final Group defaultGroup;
    private final List<TristateEntry> defaultPermissions;

    private final Map<Group, Map<FlagBitSet, Tristate>> groupPermCache;
    private final Map<FlagBitSet, Tristate> defaultPermCache;
    private final Map<Set<Group>, Map<FlagBitSet, Tristate>> groupSetPermCache;

    public GroupHandler(HandlerData data) {
        this(data,
                new ArrayList<>(),
                new HashMap<>(),
                new Group("default", "", TextColors.RED, "Default"),
                new ArrayList<>());
    }

    public GroupHandler(HandlerData data,
                        List<Group> groups,
                        Map<Group, List<TristateEntry>> groupPermissions,
                        Group defaultGroup,
                        List<TristateEntry> defaultPermissions) {
        super(data);
        this.groups = groups;
        this.defaultGroup = defaultGroup;

        this.groupPermissions = groupPermissions;
        this.defaultPermissions = defaultPermissions;

        this.groupPermCache = new CacheMap<>((k1, m1) -> {
            if (k1 instanceof Group) {
                List<TristateEntry> entries = GroupHandler.this.groupPermissions.get(k1);
                Map<FlagBitSet, Tristate> map = new CacheMap<>((k2, m2) -> {
                    if (k2 instanceof FlagBitSet) {
                        FlagBitSet flags = (FlagBitSet) k2;
                        Tristate state = UNDEFINED;
                        for (TristateEntry entry : entries) {
                            if (flags.toFlagSet().containsAll(entry.set)) {
                                state = entry.tristate;
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
                Tristate state = UNDEFINED;
                for (TristateEntry entry : GroupHandler.this.defaultPermissions) {
                    if (flags.toFlagSet().containsAll(entry.set)) {
                        state = entry.tristate;
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
                list.sort(Comparator.comparingInt(this.groups::indexOf));
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
    }

    public static boolean isNameValid(String name) {
        return !name.matches("^.*[ :\\.=;\"\'\\\\/\\{\\}\\(\\)\\[\\]<>#@\\|\\?\\*].*$") &&
                !name.equalsIgnoreCase("default") &&
                !isIn(FGStorageManagerNew.FS_ILLEGAL_NAMES, name);
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
                    if (parse.flags.containsKey("permission")) {
                        String permissionString = parse.flags.get("permission");
                        if (!permissionString.isEmpty()) {
                            if (!permissionString.matches("[\\w\\-.]+") ||
                                    permissionString.matches("^.*\\.\\..*$") ||
                                    permissionString.startsWith(".") ||
                                    permissionString.endsWith(".")) {
                                return ProcessResult.of(false, Text.of("\"" + permissionString + "\" is an invalid permission string!"));
                            }
                            group.specialPermission = true;
                            group.permission = permissionString;
                        }
                    }
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
                    if (parse.flags.containsKey("permission")) {
                        String permissionString = parse.flags.get("permission");
                        if (permissionString.isEmpty()) {
                            group.specialPermission = false;
                            group.permission = "";
                        } else {
                            if (!permissionString.matches("[\\w\\-.]+") ||
                                    permissionString.matches("^.*\\.\\..*$") ||
                                    permissionString.startsWith(".") ||
                                    permissionString.endsWith(".")) {
                                return ProcessResult.of(false, Text.of("\"" + permissionString + "\" is an invalid permission string!"));
                            }
                            group.specialPermission = true;
                            group.permission = permissionString;
                        }
                    }
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
                    if (parse.flags.containsKey("permission")) {
                        String permission = parse.flags.get("permission");
                        if (FCCUtil.checkPermissionString(permission))
                            return ProcessResult.of(false, Text.of("\"" + permission + "\" is not a valid permission string!"));
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
                    List<TristateEntry> permissions = getGroupPermissions(group);
                    TristateEntry entry = new TristateEntry(flags, state);

                    for (TristateEntry existing : permissions) {
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
                    List<TristateEntry> permissions = getGroupPermissions(group);
                    if (permissions.isEmpty())
                        return ProcessResult.of(false, "There are no entries to remove in this group!");
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
                        TristateEntry entry = null;
                        for (TristateEntry existing : permissions) {
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
                    List<TristateEntry> permissions = getGroupPermissions(group);
                    if (permissions.isEmpty())
                        return ProcessResult.of(false, "There are no entries to set in this group!");
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
                    TristateEntry entry = new TristateEntry(flags, state);

                    TristateEntry original = null;
                    for (TristateEntry existing : permissions) {
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
                    List<TristateEntry> permissions = getGroupPermissions(group);
                    if (parse.args.length < 4)
                        return ProcessResult.of(false, Text.of("Must specify flags or an index to move!"));
                    if (permissions.isEmpty())
                        return ProcessResult.of(false, "There are no entries to move in this group!");
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
                        TristateEntry entry = null;
                        for (TristateEntry existing : permissions) {
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
                return Stream.of("groups", "flags")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(GROUPS_ALIASES, parse.args[0])) {
                    return Stream.of("add", "remove", "modify", "rename", "move")
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
                } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                    return Stream.of("add", "remove", "set", "move")
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
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
                                return Stream.of("=allow", "=deny", "=pass")
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
                                    return Stream.of("=allow", "=deny", "=pass", "=clear")
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
                                return Stream.of("allow", "deny", "pass", "clear")
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            } catch (NumberFormatException ignored) {
                            }
                            if (parse.current.token.startsWith("=")) {
                                return Stream.of("=allow", "=deny", "=pass", "=clear")
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
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY)) {
            if (isIn(GROUPS_ALIASES, parse.args[0])) {
                return Stream.of("index", "color", "displayname", "permission")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(FLAGS_ALIASES, parse.args[0])) {
                return Stream.of("index")
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
        if (user == null) return EventResult.pass();
        else {
            Set<Group> set = new HashSet<>();
            final String prefix = "foxguard.handler." + this.name.toLowerCase() + ".";
            for (Group g : this.groups) {
                if (g.specialPermission) {
                    if (user.hasPermission(g.permission)) set.add(g);
                } else {
                    if (user.hasPermission(prefix + g.name)) set.add(g);
                }
            }
            return EventResult.of(this.groupSetPermCache.get(set).get(flags));
        }
    }

    @Override
    public String getShortTypeName() {
        return "Group";
    }

    @Override
    public String getLongTypeName() {
        return "Group";
    }

    @Override
    public String getUniqueTypeString() {
        return "group";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.getName() + " group add "),
                TextActions.showText(Text.of("Click to add a group")),
                "----- Group Permission Strings -----\n"));
        for (Group group : groups) {
            builder.append(Text.of(group.color,
                    TextActions.suggestCommand("/foxguard md h " + this.getName() + " group modify " + group.name + " "),
                    TextActions.showText(Text.of("Click to modify \"", group.color, group.displayName, TextColors.RESET, "\"" + (group.name.equals(group.displayName) ? "" : " (" + group.name + ")"))),
                    group.displayName,
                    TextColors.RESET, ": "));
            Text.Builder permBuilder = Text.builder();
            if (group.specialPermission) {
                permBuilder.append(Text.of(group.permission));
            } else {
                permBuilder.append(Text.of("foxguard.handler.", TextColors.YELLOW, this.name.toLowerCase(), TextColors.RESET, ".", group.color, group.name));
            }
            permBuilder.onHover(TextActions.showText(Text.of("Click to modify the permissions string for \"", group.color, group.displayName, TextColors.RESET, "\"" + (group.name.equals(group.displayName) ? "" : " (" + group.name + ")"))));
            permBuilder.onClick(TextActions.suggestCommand("/foxguard md h " + this.getName() + " group modify " + group.name + " --p:"));
            builder.append(permBuilder.build());
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
            for (TristateEntry entry : this.groupPermissions.get(group)) {
                StringBuilder stringBuilder = new StringBuilder();
                entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
                Text.Builder entryBuilder = Text.builder();
                entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "))
                        .append(FGUtil.readableTristateText(entry.tristate))
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
        for (TristateEntry entry : this.defaultPermissions) {
            StringBuilder stringBuilder = new StringBuilder();
            entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
            Text.Builder entryBuilder = Text.builder();
            entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "))
                    .append(FGUtil.readableTristateText(entry.tristate))
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
        try (DB flagMapDB = FGStorageManagerOld.openFoxDB(directory.resolve("groups.foxdb"))) {
            List<String> groupNames = flagMapDB.indexTreeList("names", Serializer.STRING).createOrOpen();
            groupNames.clear();
            groupNames.addAll(this.groups.stream().map(group -> group.name).collect(Collectors.toList()));
        }
        try (DB flagMapDB = FGStorageManagerOld.openFoxDB(directory.resolve("flags.foxdb"))) {
            for (Group group : this.groups) {
                List<String> stringEntries = flagMapDB.indexTreeList(group.name, Serializer.STRING).createOrOpen();
                stringEntries.clear();
                stringEntries.addAll(this.groupPermissions.get(group).stream().map(TristateEntry::serialize).collect(Collectors.toList()));
            }
            List<String> stringEntries = flagMapDB.indexTreeList("default", Serializer.STRING).createOrOpen();
            stringEntries.clear();
            stringEntries.addAll(this.defaultPermissions.stream().map(TristateEntry::serialize).collect(Collectors.toList()));
        }
        {
            Path groupsFile = directory.resolve("groups.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(groupsFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(groupsFile, loader);
            CommentedConfigurationNode defaultNode = root.getNode("default");
            defaultNode.getNode("displayname").setValue(this.defaultGroup.displayName);
            defaultNode.getNode("color").setValue(this.defaultGroup.color.getName());
            CommentedConfigurationNode groupsNode = root.getNode("groups");
            for (Group group : this.groups) {
                CommentedConfigurationNode groupNode = groupsNode.getNode(group.name);
                groupNode.getNode("displayname").setValue(group.displayName);
                groupNode.getNode("color").setValue(group.color.getName());
                groupNode.getNode("permission").setValue(group.permission);
            }
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
            Set<Set<Group>> groupSuperSet = new HashSet<>();
            for (Map.Entry<Set<Group>, Map<FlagBitSet, Tristate>> entry : this.groupSetPermCache.entrySet()) {
                Set<Group> key = entry.getKey();
                if (key.contains(group)) groupSuperSet.add(key);
            }
            groupSuperSet.forEach(this.groupSetPermCache::remove);
            this.groups.remove(group);
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

    public boolean addFlagEntry(Group group, TristateEntry entry) {
        return addFlagEntry(group, 0, entry);
    }

    public boolean addFlagEntry(Group group, int index, TristateEntry entry) {
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index > groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + groupEntries.size());
        for (TristateEntry groupEntry : groupEntries) {
            if (groupEntry.set.equals(entry.set)) return false;
        }
        groupEntries.add(index, entry);
        clearFlagCacheForGroup(group);
        return true;
    }

    public void setFlagEntry(Group group, TristateEntry entry) {
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        for (TristateEntry groupEntry : groupEntries) {
            if (groupEntry.set.equals(entry.set)) {
                groupEntry.tristate = entry.tristate;
                return;
            }
        }
        groupEntries.add(entry);
        clearFlagCacheForGroup(group);
    }

    public void setFlagEntry(Group group, int index, TristateEntry entry) {
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        TristateEntry original = null;
        for (TristateEntry groupEntry : groupEntries) {
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
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index >= groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (groupEntries.size() - 1));
        TristateEntry entry = groupEntries.get(index);
        entry.tristate = state;
        clearFlagCacheForGroup(group);
    }

    public boolean removeFlagEntry(Group group, Set<Flag> flags) {
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        TristateEntry toRemove = null;
        for (TristateEntry groupEntry : groupEntries) {
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
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        if (index < 0 || index >= groupEntries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (groupEntries.size() - 1));
        groupEntries.remove(index);
        clearFlagCacheForGroup(group);
    }

    public boolean moveFlagEntry(Group group, Set<Flag> flags, int destination) {
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        if (destination < 0 || destination >= groupEntries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (groupEntries.size() - 1));
        TristateEntry toMove = null;
        for (TristateEntry groupEntry : groupEntries) {
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
        List<TristateEntry> groupEntries = getGroupPermissions(group);
        if (source < 0 || source >= groupEntries.size())
            throw new IndexOutOfBoundsException("Source index out of bounds: " + source + " Range: 0-" + (groupEntries.size() - 1));
        if (destination < 0 || destination >= groupEntries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (groupEntries.size() - 1));
        TristateEntry entry = groupEntries.remove(source);
        groupEntries.add(destination, entry);
        clearFlagCacheForGroup(group);
    }

    private void clearFlagCacheForGroup(Group group) {
        if (group == defaultGroup) {
            this.defaultPermCache.clear();
            this.groupSetPermCache.clear();
        } else {
            this.groupPermCache.get(group).clear();
            Set<Set<Group>> groupSuperSet = new HashSet<>();
            for (Map.Entry<Set<Group>, Map<FlagBitSet, Tristate>> cacheEntry : this.groupSetPermCache.entrySet()) {
                Set<Group> key = cacheEntry.getKey();
                if (key.contains(group)) groupSuperSet.add(key);
            }
            groupSuperSet.forEach(this.groupSetPermCache::remove);
        }

    }

    private List<TristateEntry> getGroupPermissions(Group group) {
        if (group == this.defaultGroup) return this.defaultPermissions;
        else return this.groupPermissions.get(group);
    }

    private boolean userFilter(Operation op, boolean isPresent) {
        switch (op) {
            case ADD:
                return !isPresent;
            case REMOVE:
                return isPresent;
            case SET:
                return true;
            default:
                return false;
        }
    }

    public static class Group {
        private String name;
        private String displayName;
        private TextColor color;
        private String permission;
        private boolean specialPermission;

        private Group(String name) {
            this(name, "");
        }

        private Group(String name, String permission) {
            this(name, permission, TextColors.WHITE, name);
        }

        private Group(String name, String permission, TextColor color, String displayName) {
            this.name = name.toLowerCase();
            this.displayName = displayName;
            this.color = color;
            this.permission = permission;
            this.specialPermission = permission != null && !permission.isEmpty();
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

        public String getPermission() {
            return permission;
        }
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] ALIASES = {"group", "permgroup"};

        @Override
        public IHandler create(String name, String arguments, CommandSource source) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
            GroupHandler handler = new GroupHandler(new HandlerData().setName(name));
            if (parse.args.length < 1 || !parse.args[0].equalsIgnoreCase("bare")) {
                Group members = handler.createGroup("members").get();
                members.displayName = "Members";
                members.color = TextColors.GREEN;
            }
            return handler;
        }

        @Override
        public IHandler create(Path directory, HandlerData data) {
            if (Files.exists(directory.resolve("groups.foxdb")) || Files.exists(directory.resolve("flags.foxdb"))) {
                return createOld(directory, data);
            }
            return null;
        }

        public IHandler createOld(Path directory, HandlerData data) {
            List<String> groupNames = new ArrayList<>();
            try (DB flagMapDB = FGStorageManagerOld.openFoxDB(directory.resolve("groups.foxdb"))) {
                groupNames.addAll(flagMapDB.indexTreeList("names", Serializer.STRING).createOrOpen());
            }
            List<Group> groups = new ArrayList<>();
            Path groupsFile = directory.resolve("groups.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(groupsFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(groupsFile, loader);
            CommentedConfigurationNode groupsNode = root.getNode("groups");
            for (String groupName : groupNames) {
                CommentedConfigurationNode groupNode = groupsNode.getNode(groupName);
                String displayName = groupNode.getNode("displayname").getString(groupName);
                TextColor color = Sponge.getRegistry().getType(TextColor.class, groupNode.getNode("color").getString("white")).orElse(TextColors.WHITE);
                String permission = groupNode.getNode("permission").getString("");
                groups.add(new Group(groupName, permission, color, displayName));
            }
            CommentedConfigurationNode defaultNode = root.getNode("default");
            String defaultDisplayName = defaultNode.getNode("displayname").getString("Default");
            TextColor defaultColor = Sponge.getRegistry().getType(TextColor.class, defaultNode.getNode("color").getString("red")).orElse(TextColors.RED);

            Map<Group, List<TristateEntry>> groupPermissions = new HashMap<>();
            List<TristateEntry> defaultPermissions;
            try (DB flagMapDB = FGStorageManagerOld.openFoxDB(directory.resolve("flags.foxdb"))) {
                for (Group group : groups) {
                    List<String> stringEntries = flagMapDB.indexTreeList(group.name, Serializer.STRING).createOrOpen();
                    groupPermissions.put(group, stringEntries.stream()
                            .map(TristateEntry::deserialize)
                            .collect(Collectors.toList()));
                }
                List<String> stringEntries = flagMapDB.indexTreeList("default", Serializer.STRING).createOrOpen();
                defaultPermissions = stringEntries.stream().map(TristateEntry::deserialize).collect(Collectors.toList());
            }

            return new GroupHandler(data,
                    groups,
                    groupPermissions,
                    new Group("default", "", defaultColor, defaultDisplayName),
                    defaultPermissions);
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "group";
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
                return Stream.of("bare")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
            return ImmutableList.of();
        }
    }
}
