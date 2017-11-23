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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.PermissionEntry;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

/**
 * Created by Fox on 11/15/2016.
 */
public class PermissionHandler extends HandlerBase {

    private final List<PermissionEntry> entries;
    private final Map<FlagBitSet, List<String>> permCache;
    private String defaultPermission;

    public PermissionHandler(HandlerData data) {
        this(data,
                new ArrayList<>(), "");
    }

    public PermissionHandler(HandlerData data, List<PermissionEntry> entries, String defaultPermission) {
        super(data);
        this.entries = entries;
        this.defaultPermission = defaultPermission;
        this.permCache = new CacheMap<>((k, m) -> {
            if (k instanceof FlagBitSet) {
                FlagBitSet flags = (FlagBitSet) k;
                List<String> perms = new ArrayList<>();
                final String prefix = "foxguard.handler." + this.name;
                for (PermissionEntry entry : entries) {
                    if (flags.toFlagSet().containsAll(entry.set)) {
                        perms.add(expandPermission(entry.permission));
                    }
                }
                perms.add(expandPermission(defaultPermission));
                m.put(flags, perms);
                return perms;
            } else return null;
        });
    }

    @Override
    public String getShortTypeName() {
        return "Perm";
    }

    @Override
    public String getLongTypeName() {
        return "Permission";
    }

    @Override
    public String getUniqueTypeString() {
        return "permission";
    }

    @Override
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        if (user == null) return EventResult.pass();
        for (String permission : this.permCache.get(flags)) {
            if (user.hasPermission(permission + ".allow")) return EventResult.allow();
            if (user.hasPermission(permission + ".deny")) return EventResult.deny();
            if (user.hasPermission(permission + ".pass")) return EventResult.pass();
        }
        return EventResult.pass();
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify a command!"));

        if (isIn(ENTRIES_ALIASES, parse.args[0]) || isIn(FLAGS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an entry operation!"));
            switch (parse.args[1].toLowerCase()) {
                case "add": {
                    String perm = null;
                    Set<Flag> flags = new HashSet<>();
                    boolean areFlagsSet = false;
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify flags and a permission string!"));
                    for (int i = 2; i < parse.args.length; i++) {
                        String argument = parse.args[i];
                        if (argument.startsWith("=")) {
                            argument = argument.substring(1);
                            if (checkPermissionString(argument)) {
                                perm = argument;
                            } else {
                                return ProcessResult.of(false, "\"" + argument + "\" is not a valid permission!");
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
                    if (perm == null) return ProcessResult.of(false, Text.of("Must specify a permission string!"));
                    PermissionEntry entry = new PermissionEntry(flags, perm);

                    for (PermissionEntry existing : this.entries) {
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
                                else if (index > this.entries.size()) index = this.entries.size();
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    this.addFlagEntry(index, entry);
                    return ProcessResult.of(true, Text.of("Successfully added flag entry!"));
                }
                case "remove": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify flags or an index to remove!"));
                    if (this.entries.isEmpty()) return ProcessResult.of(false, "There are no entries to remove!");
                    try {
                        int index = Integer.parseInt(parse.args[2]);
                        if (index < 0) index = 0;
                        else if (index >= this.entries.size()) index = this.entries.size() - 1;
                        removeFlagEntry(index);
                        return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                    } catch (NumberFormatException ignored) {
                        Set<Flag> flags = new HashSet<>();
                        for (int i = 2; i < parse.args.length; i++) {
                            String argument = parse.args[i];
                            Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                            if (flagOptional.isPresent()) {
                                flags.add(flagOptional.get());
                            } else {
                                return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                            }
                        }
                        PermissionEntry entry = null;
                        for (PermissionEntry existing : this.entries) {
                            if (existing.set.equals(flags)) {
                                entry = existing;
                                break;
                            }
                        }
                        if (entry == null) return ProcessResult.of(false, Text.of("No flag entry with these flags!"));
                        removeFlagEntry(this.entries.indexOf(entry));
                        return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                    }
                }
                case "set": {
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify an index or flags and then a tristate value!"));
                    if (this.entries.isEmpty()) return ProcessResult.of(false, "There are no entries to set!");
                    try {
                        int index = Integer.parseInt(parse.args[2]);
                        if (index < 0) index = 0;
                        else if (index >= this.entries.size()) index = this.entries.size() - 1;
                        if (parse.args.length < 4)
                            return ProcessResult.of(false, Text.of("Must specify a permission string!"));
                        String perm = parse.args[3];
                        if (perm.startsWith("=")) perm = perm.substring(1);
                        if (checkPermissionString(perm)) {
                            this.setFlagEntry(index, perm);
                            return ProcessResult.of(true, "Successfully updated entry!");
                        } else {
                            return ProcessResult.of(false, "\"" + perm + "\" is not a valid permission!");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    String perm = null;
                    Set<Flag> flags = new HashSet<>();
                    boolean areFlagsSet = false;
                    for (int i = 2; i < parse.args.length; i++) {
                        String argument = parse.args[i];
                        if (argument.startsWith("=")) {
                            argument = argument.substring(1);
                            if (checkPermissionString(argument)) {
                                perm = argument;
                            } else {
                                return ProcessResult.of(false, "\"" + argument + "\" is not a valid permission!");
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
                    if (perm == null) return ProcessResult.of(false, Text.of("Must specify a permission string!"));
                    PermissionEntry entry = new PermissionEntry(flags, perm);

                    int index = 0;
                    if (parse.flags.containsKey("index")) {
                        String number = parse.flags.get("index");
                        if (!number.isEmpty()) {
                            try {
                                index = Integer.parseInt(number);
                                if (index < 0) index = 0;
                                else if (index > this.entries.size()) index = this.entries.size();
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    this.setFlagEntry(index, entry);
                    return ProcessResult.of(true, Text.of("Successfully set flag entry!"));

                }
                case "move": {
                    if (parse.args.length < 2) {
                        return ProcessResult.of(false, Text.of("Must specify flags or an index to move!"));
                    }
                    if (this.entries.isEmpty()) return ProcessResult.of(false, "There are no entries to move!");
                    try {
                        int from = Integer.parseInt(parse.args[1]);
                        if (from < 0) from = 0;
                        else if (from >= this.entries.size()) from = this.entries.size() - 1;
                        if (parse.args.length < 3)
                            return ProcessResult.of(false, Text.of("Must specify a target index to move to!"));
                        int to = FCCUtil.parseCoordinate(from, parse.args[2]);
                        if (to < 0) to = 0;
                        else if (to >= this.entries.size()) to = this.entries.size() - 1;
                        moveFlagEntry(from, to);
                        return ProcessResult.of(true, Text.of("Successfully moved flag entry!"));
                    } catch (NumberFormatException ignored) {
                        Set<Flag> flags = new HashSet<>();
                        int index = 0;
                        boolean set = false;
                        boolean relative = false;
                        for (int i = 1; i < parse.args.length; i++) {
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
                        PermissionEntry entry = null;
                        for (PermissionEntry existing : this.entries) {
                            if (existing.set.equals(flags)) {
                                entry = existing;
                                break;
                            }
                        }

                        if (entry == null) return ProcessResult.of(false, Text.of("No flag entry with these flags!"));
                        if (relative) index += this.entries.indexOf(entry);
                        moveFlagEntry(entry.set, index);
                    }
                    return ProcessResult.of(true, Text.of("Successfully moved flag entry!"));
                }
                default:
                    return ProcessResult.of(false, Text.of("Not a valid flag operation!"));
            }
        } else if (parse.args[0].equalsIgnoreCase("default")) {
            if (parse.args.length > 1) {
                String perm = parse.args[1];
                if (perm.startsWith("=")) perm = perm.substring(1);
                if (checkPermissionString(perm)) {
                    defaultPermission = perm;
                    return ProcessResult.of(true, Text.of(TextColors.GREEN, "Successfully set default permission to ",
                            TextColors.AQUA, "\"",
                            TextColors.RESET, expandPermission(perm),
                            TextColors.AQUA, "\"",
                            TextColors.GREEN, "!"));
                } else {
                    return ProcessResult.of(false, "\"" + perm + "\" is not a valid permission!");
                }
            } else {
                defaultPermission = "";
                return ProcessResult.of(true, "Successfully reset default permission!");
            }
        } else {
            return ProcessResult.of(false, "Not a valid modification command!");
        }
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();

        if (parse.current.type == AdvCmdParser.CurrentElement.ElementType.ARGUMENT) {
            if (parse.current.index == 0) {
                return Stream.of("entries", "default")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(ENTRIES_ALIASES, parse.args[0]) || isIn(FLAGS_ALIASES, parse.args[0])) {
                if (parse.current.index == 1) {
                    return Stream.of("add", "set", "remove", "move")
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.current.index > 1) {
                    switch (parse.args[1].toLowerCase()) {
                        case "add": {
                            if (parse.current.token.startsWith("=")) {
                                String perm = parse.current.token.substring(1);
                                if (!perm.isEmpty()) {
                                    if (checkPermissionString(perm)) {
                                        source.sendMessage(Text.of(TextColors.GREEN, "Permission is valid!"));
                                    } else {
                                        source.sendMessage(Text.of(TextColors.RED, "Permission is not valid!"));
                                    }
                                }
                                return ImmutableList.of();
                            } else {
                                String[] flagArgs = Arrays.copyOfRange(parse.args, 2, parse.args.length);
                                return FlagRegistry.getInstance().getFlagList().stream()
                                        .map(Flag::getName)
                                        .filter(arg -> !isIn(flagArgs, arg))
                                        .filter(new StartsWithPredicate(parse.current.token))
                                        .map(args -> parse.current.prefix + args)
                                        .collect(GuavaCollectors.toImmutableList());
                            }
                        }
                        case "set": {
                            if (parse.current.index == 2) {
                                if (parse.current.token.startsWith("=")) {
                                    String perm = parse.current.token.substring(1);
                                    if (!perm.isEmpty()) {
                                        if (checkPermissionString(perm)) {
                                            source.sendMessage(Text.of(TextColors.GREEN, "Permission is valid!"));
                                        } else {
                                            source.sendMessage(Text.of(TextColors.RED, "Permission is not valid!"));
                                        }
                                    }
                                    return ImmutableList.of();
                                } else {
                                    return FlagRegistry.getInstance().getFlagList().stream()
                                            .map(Flag::getName)
                                            .filter(new StartsWithPredicate(parse.current.token))
                                            .map(args -> parse.current.prefix + args)
                                            .collect(GuavaCollectors.toImmutableList());
                                }
                            } else if (parse.current.index == 3) try {
                                Integer.parseInt(parse.args[2]);
                                String perm = parse.current.token.substring(1);
                                if (!perm.isEmpty()) {
                                    if (checkPermissionString(perm)) {
                                        source.sendMessage(Text.of(TextColors.GREEN, "Permission is valid!"));
                                    } else {
                                        source.sendMessage(Text.of(TextColors.RED, "Permission is not valid!"));
                                    }
                                }
                                return ImmutableList.of();
                            } catch (NumberFormatException ignored) {
                            }
                            if (parse.current.token.startsWith("=")) {
                                String perm = parse.current.token.substring(1);
                                if (!perm.isEmpty()) {
                                    if (checkPermissionString(perm)) {
                                        source.sendMessage(Text.of(TextColors.GREEN, "Permission is valid!"));
                                    } else {
                                        source.sendMessage(Text.of(TextColors.RED, "Permission is not valid!"));
                                    }
                                }
                                return ImmutableList.of();
                            } else {
                                String[] flagArgs = Arrays.copyOfRange(parse.args, 2, parse.args.length);
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
                            String[] flagArgs = Arrays.copyOfRange(parse.args, 2, parse.args.length);
                            return FlagRegistry.getInstance().getFlagList().stream()
                                    .map(Flag::getName)
                                    .filter(arg -> !isIn(flagArgs, arg))
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        }
                    }
                }
            } else if (parse.args[0].equalsIgnoreCase("default")) {
                if (parse.current.index == 1 && !parse.current.token.isEmpty()) {
                    String perm = parse.current.token;
                    if (perm.startsWith("=")) perm = perm.substring(1);
                    if (checkPermissionString(perm)) {
                        source.sendMessage(Text.of(TextColors.GREEN, "Permission is valid!"));
                    } else {
                        source.sendMessage(Text.of(TextColors.RED, "Permission is not valid!"));
                    }
                    return ImmutableList.of();
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();

        Text prefix = Text.of("foxguard.handler.", TextColors.GOLD, this.name.toLowerCase());
        Text postfix = Text.of(".",
                TextColors.AQUA, "<",
                TextColors.GREEN, "allow",
                TextColors.AQUA, "/",
                TextColors.RED, "deny",
                TextColors.AQUA, "/",
                TextColors.YELLOW, "pass",
                TextColors.AQUA, ">");
        builder.append(Text.of(
                TextActions.showText(Text.of("Click to add a permission entry")),
                TextActions.suggestCommand("/foxguard md h " + this.getFullName() + " entries add "),
                TextColors.GREEN, "Entries:"
        ));
        int index = 0;
        for (PermissionEntry entry : this.entries) {
            StringBuilder stringBuilder = new StringBuilder();
            entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
            Text.Builder entryBuilder = Text.builder();
            entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "));
            if (entry.permission.isEmpty()) {
                entryBuilder.append(prefix);
            } else if (entry.permission.startsWith(".")) {
                entryBuilder.append(prefix);
                entryBuilder.append(Text.of(entry.permission));
            } else {
                entryBuilder.append(Text.of(entry.permission));
            }
            entryBuilder.append(postfix)
                    .onHover(TextActions.showText(Text.of("Click to change this entry")))
                    .onClick(TextActions.suggestCommand("/foxguard md h " + this.getFullName() + "entries set " + (index++) + " "));
            builder.append(Text.NEW_LINE).append(entryBuilder.build());
        }
        Text.Builder entryBuilder = Text.builder();
        entryBuilder.append(Text.of(TextColors.RED, "Default: "));
        if (defaultPermission.isEmpty()) {
            entryBuilder.append(prefix);
        } else if (defaultPermission.startsWith(".")) {
            entryBuilder.append(prefix);
            entryBuilder.append(Text.of(defaultPermission));
        } else {
            entryBuilder.append(Text.of(defaultPermission));
        }
        entryBuilder.append(postfix)
                .onHover(TextActions.showText(Text.of("Click to change this entry")))
                .onClick(TextActions.suggestCommand("/foxguard md h " + this.getFullName() + " default "));
        builder.append(Text.NEW_LINE).append(entryBuilder.build());

        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String
            arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        Path permissionFile = directory.resolve("permissions.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(permissionFile).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(permissionFile, loader);
        root.getNode("default").setValue(defaultPermission);
        root.getNode("entries").setValue(this.entries.stream()
                .map(PermissionEntry::serialize)
                .collect(Collectors.toList())
        );
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        this.permCache.clear();
    }

    public boolean addFlagEntry(PermissionEntry entry) {
        return addFlagEntry(0, entry);
    }

    public boolean addFlagEntry(int index, PermissionEntry entry) {
        if (index < 0 || index > this.entries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + this.entries.size());
        for (PermissionEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) return false;
        }
        this.entries.add(index, entry);
        this.permCache.clear();
        return true;
    }

    public void setFlagEntry(PermissionEntry entry) {
        for (PermissionEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) {
                groupEntry.permission = entry.permission;
                return;
            }
        }
        this.entries.add(entry);
        this.permCache.clear();
    }

    public void setFlagEntry(int index, PermissionEntry entry) {
        PermissionEntry original = null;
        for (PermissionEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) {
                original = groupEntry;
                break;
            }
        }
        if (original != null) this.entries.remove(original);
        this.entries.add(index, entry);
        this.permCache.clear();
    }

    public void setFlagEntry(int index, String permission) {
        if (index < 0 || index >= this.entries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (this.entries.size() - 1));
        PermissionEntry entry = this.entries.get(index);
        entry.permission = permission;
        this.permCache.clear();
    }

    public boolean removeFlagEntry(Set<Flag> flags) {
        PermissionEntry toRemove = null;
        for (PermissionEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(flags)) {
                toRemove = groupEntry;
            }
        }
        if (toRemove == null) return false;
        this.entries.remove(toRemove);
        this.permCache.clear();
        return true;
    }

    public void removeFlagEntry(int index) {
        if (index < 0 || index >= this.entries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (this.entries.size() - 1));
        this.entries.remove(index);
        this.permCache.clear();
    }

    public boolean moveFlagEntry(Set<Flag> flags, int destination) {
        if (destination < 0 || destination >= this.entries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (this.entries.size() - 1));
        PermissionEntry toMove = null;
        for (PermissionEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(flags)) {
                toMove = groupEntry;
            }
        }
        if (toMove == null) return false;
        this.entries.remove(toMove);
        this.permCache.clear();
        return true;
    }

    public void moveFlagEntry(int source, int destination) {
        if (source < 0 || source >= this.entries.size())
            throw new IndexOutOfBoundsException("Source index out of bounds: " + source + " Range: 0-" + (this.entries.size() - 1));
        if (destination < 0 || destination >= this.entries.size())
            throw new IndexOutOfBoundsException("Destination index out of bounds: " + destination + " Range: 0-" + (this.entries.size() - 1));
        PermissionEntry entry = this.entries.remove(source);
        this.entries.add(destination, entry);
        this.permCache.clear();
    }

    private boolean checkPermissionString(String perm) {
        return !(!perm.isEmpty() && (!perm.matches("[\\w\\-.]+") ||
                perm.matches("^.*\\.\\..*$") ||
                perm.endsWith(".")));
    }

    private String expandPermission(String perm) {
        if (perm.isEmpty() || perm.startsWith(".")) perm = "foxguard.handler." + this.name.toLowerCase() + perm;
        return perm;
    }

    public static class Factory implements IHandlerFactory {

        public static final String[] ALIASES = {"perm", "perms", "permission", "permissions"};

        @Override
        public IHandler create(String name, String arguments, CommandSource source) throws CommandException {
            return new PermissionHandler(new HandlerData().setName(name));
        }

        @Override
        public IHandler create(Path directory, HandlerData data) {
            Path permissionsFile = directory.resolve("permissions.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(permissionsFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(permissionsFile, loader);
            List<Optional<String>> optionalFlagsList = root.getNode("entries").getList(o -> {
                if (o instanceof String) {
                    return Optional.of((String) o);
                } else return Optional.empty();
            });
            List<PermissionEntry> entries = optionalFlagsList.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(PermissionEntry::deserialize)
                    .collect(Collectors.toList());
            String defaultPermission = root.getNode("default").getString("");
            return new PermissionHandler(data, entries, defaultPermission);
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "permission";
        }

        @Override
        public String getPrimaryAlias() {
            return getType();
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return ImmutableList.of();
        }
    }
}
