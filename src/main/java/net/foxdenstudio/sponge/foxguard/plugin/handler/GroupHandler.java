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
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class GroupHandler extends HandlerBase {

    private final Map<IFlag, Tristate> groupPermissions;
    private final Map<IFlag, Tristate> defaultPermissions;

    private final Map<IFlag, Tristate> groupPermCache;
    private final Map<IFlag, Tristate> defaultPermCache;

    private PassiveOptions passiveOption = PassiveOptions.PASSTHROUGH;

    public GroupHandler(String name, int priority) {
        this(name, priority,
                new CacheMap<>((o, m) -> Tristate.UNDEFINED),
                new CacheMap<>((o, m) -> Tristate.UNDEFINED));
    }

    public GroupHandler(String name, int priority,
                        Map<IFlag, Tristate> groupPermissions,
                        Map<IFlag, Tristate> defaultPermissions) {
        super(name, priority);
        this.groupPermissions = groupPermissions;
        this.defaultPermissions = defaultPermissions;

        this.groupPermCache = new CacheMap<>((o, m) -> {
            if (o instanceof Flag) {
                Tristate state = groupPermissions.get(FGUtil.nearestParent((Flag) o, groupPermissions.keySet()));
                m.put((Flag) o, state);
                return state;
            } else return Tristate.UNDEFINED;
        });
        this.defaultPermCache = new CacheMap<>((o, m) -> {
            if (o instanceof Flag) {
                Tristate state = defaultPermissions.get(FGUtil.nearestParent((Flag) o, defaultPermissions.keySet()));
                m.put((Flag) o, state);
                return state;
            } else return Tristate.UNDEFINED;
        });
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();
        if (parse.args.length > 0) {
            if (isIn(SET_ALIASES, parse.args[0])) {
                Map<IFlag, Tristate> map;
                if (parse.args.length > 1) {
                    if (isIn(GROUPS_ALIASES, parse.args[1])) {
                        map = groupPermissions;
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
                    } else if (isIn(GROUPS_ALIASES, parse.args[1])) {
                        this.passiveOption = PassiveOptions.GROUP;
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
                return ProcessResult.of(false, Text.of("Not a valid GroupHandler command!"));
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
                return ImmutableList.of("set", "passive").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(SET_ALIASES, parse.args[0])) {
                    return Flag.getFlags().stream()
                            .map(IFlag::flagName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(PASSIVE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false", "passthrough", "group", "default").stream()
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
    public EventResult handle(User user, IFlag flag, Optional<Event> event, Object... extra) {
        if (user == null) {
            switch (this.passiveOption) {
                case GROUP:
                    return EventResult.of(this.groupPermCache.get(flag));
                case DEFAULT:
                    return EventResult.of(this.defaultPermCache.get(flag));
                case ALLOW:
                    return EventResult.allow();
                case DENY:
                    return EventResult.deny();
                case PASSTHROUGH:
                    return EventResult.pass();
            }
        }
        if (user.hasPermission("foxguard.handler." + this.name.toLowerCase()))
            return EventResult.of(this.groupPermCache.get(flag));
        else return EventResult.of(this.defaultPermCache.get(flag));
    }

    private void clearCache() {
        this.groupPermCache.clear();
        this.defaultPermCache.clear();
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
        builder.append(Text.of(TextColors.GOLD, "Permission:\n"))
                .append(Text.of(TextColors.RESET, "foxguard.handler."))
                .append(Text.of(TextColors.YELLOW, this.getName()))
                .append(Text.of("\n"));
        builder.append(Text.of(TextColors.GOLD,
                TextActions.suggestCommand("/foxguard md h " + this.name + " set group "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Group permissions:\n"));
        for (IFlag f : this.groupPermissions.keySet().stream().sorted().collect(GuavaCollectors.toImmutableList())) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCUtil.readableTristateText(groupPermissions.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " set group " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        builder.append(Text.of(TextColors.RED,
                TextActions.suggestCommand("/foxguard md h " + this.name + " set default "),
                TextActions.showText(Text.of("Click to Set a Flag")),
                "Default permissions:\n"));
        for (IFlag f : this.defaultPermissions.keySet().stream().sorted().collect(GuavaCollectors.toImmutableList())) {
            builder.append(
                    Text.builder().append(Text.of("  " + f.toString() + ": "))
                            .append(FCUtil.readableTristateText(defaultPermissions.get(f)))
                            .append(Text.of("\n"))
                            .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " set default " + f.flagName() + " "))
                            .onHover(TextActions.showText(Text.of("Click to Change This Flag")))
                            .build()
            );
        }
        builder.append(Text.builder()
                .append(Text.of(TextColors.AQUA, "Passive setting: "))
                .append(Text.of(TextColors.RESET, this.passiveOption.toString()))
                .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " passive "))
                .onHover(TextActions.showText(Text.of("Click to Change Passive Setting"))).build()
        );
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
            Map<String, String> groupStorageFlagMap = flagMapDB.hashMap("group", Serializer.STRING, Serializer.STRING).createOrOpen();
            groupStorageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : groupPermissions.entrySet()) {
                groupStorageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
            Map<String, String> defaultStorageFlagMap = flagMapDB.hashMap("default", Serializer.STRING, Serializer.STRING).createOrOpen();
            defaultStorageFlagMap.clear();
            for (Map.Entry<IFlag, Tristate> entry : defaultPermissions.entrySet()) {
                defaultStorageFlagMap.put(entry.getKey().flagName(), entry.getValue().name());
            }
            Atomic.String passiveOptionString = flagMapDB.atomicString("passive").createOrOpen();
            passiveOptionString.set(passiveOption.name());
        }
    }

    public PassiveOptions getPassiveOption() {
        return passiveOption;
    }

    public void setPassiveOption(PassiveOptions passiveOption) {
        this.passiveOption = passiveOption;
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
                    return "Group";
                case DEFAULT:
                    return "Default";
                default:
                    return "Awut...?";
            }
        }
    }

    public enum UserOperations {
        ADD,
        REMOVE,
        SET
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] groupAliases = {"group", "groups"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) {
            GroupHandler handler = new GroupHandler(name, priority);
            return handler;
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            Map<IFlag, Tristate> groupPermissions = new CacheMap<>((k, m) -> Tristate.UNDEFINED);
            Map<IFlag, Tristate> defaultPermissions = new CacheMap<>((k, m) -> Tristate.UNDEFINED);
            PassiveOptions option = PassiveOptions.PASSTHROUGH;
            try (DB flagMapDB = DBMaker.fileDB(directory.resolve("flags.db").normalize().toString()).make()) {
                Map<String, String> groupStorageFlagMap = flagMapDB.hashMap("group", Serializer.STRING, Serializer.STRING).createOrOpen();
                for (Map.Entry<String, String> entry : groupStorageFlagMap.entrySet()) {
                    IFlag flag = Flag.flagFrom(entry.getKey());
                    if (flag != null) {
                        Tristate state = Tristate.valueOf(entry.getValue());
                        if (state != null) {
                            groupPermissions.put(flag, state);
                        }
                    }
                }
                Map<String, String> defaultStorageFlagMap = flagMapDB.hashMap("default", Serializer.STRING, Serializer.STRING).createOrOpen();
                for (Map.Entry<String, String> entry : defaultStorageFlagMap.entrySet()) {
                    IFlag flag = Flag.flagFrom(entry.getKey());
                    if (flag != null) {
                        Tristate state = Tristate.valueOf(entry.getValue());
                        if (state != null) {
                            defaultPermissions.put(flag, state);
                        }
                    }
                }
                Atomic.String passiveOptionString = flagMapDB.atomicString("passive").createOrOpen();
                try {
                    PassiveOptions po = PassiveOptions.valueOf(passiveOptionString.get());
                    if (po != null) option = po;
                } catch (IllegalArgumentException ignored) {
                }
            }
            GroupHandler handler = new GroupHandler(name, priority, groupPermissions, defaultPermissions);
            handler.setPassiveOption(option);
            return handler;
        }

        @Override
        public String[] getAliases() {
            return groupAliases;
        }

        @Override
        public String getType() {
            return "group";
        }

        @Override
        public String getPrimaryAlias() {
            return "group";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }
}
