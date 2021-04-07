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

package net.foxdenstudio.sponge.foxguard.pluginold.handler;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.pluginold.flag.Flag;
import net.foxdenstudio.sponge.foxguard.pluginold.flag.FlagSet;
import net.foxdenstudio.sponge.foxguard.pluginold.flag.FlagRegistry;
import net.foxdenstudio.sponge.foxguard.pluginold.handler.util.TristateEntry;
import net.foxdenstudio.sponge.foxguard.pluginold.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.pluginold.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.pluginold.util.ExtraContext;
import net.foxdenstudio.sponge.foxguard.pluginold.util.FGUtil;
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
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class StaticHandler extends HandlerBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(INDEX_ALIASES, key) && !map.containsKey("index")) {
            map.put("index", value);
        }
        return true;
    };

    private final List<TristateEntry> entries;
    private final Map<FlagSet, Tristate> permCache;

    public StaticHandler(String name, int priority) {
        this(name, priority, true);
    }

    public StaticHandler(String name, int priority, boolean isEnabled) {
        super(name, priority, isEnabled);
        this.entries = new ArrayList<>();
        this.permCache = new CacheMap<>((k, m) -> {
            if (k instanceof FlagSet) {
                FlagSet flags = (FlagSet) k;
                Tristate state = Tristate.UNDEFINED;
                for (TristateEntry entry : StaticHandler.this.entries) {
                    if (flags.toFlagSet().containsAll(entry.set)) {
                        state = entry.tristate;
                        break;
                    }
                }
                m.put(flags, state);
                return state;
            } else return null;
        });
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getShortTypeName() {
        return "Static";
    }

    @Override
    public String getLongTypeName() {
        return "Static";
    }

    @Override
    public String getUniqueTypeString() {
        return "static";
    }

    @Override
    public EventResult handle(@Nullable User user, FlagSet flags, ExtraContext extra) {
        return EventResult.of(this.permCache.get(flags));
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify an command!"));

        switch (parse.args[0].toLowerCase()) {
            case "add": {
                Tristate state = null;
                Set<Flag> flags = new HashSet<>();
                boolean areFlagsSet = false;
                if (parse.args.length < 2)
                    return ProcessResult.of(false, Text.of("Must specify flags and a tristate value!"));
                for (int i = 1; i < parse.args.length; i++) {
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
                TristateEntry entry = new TristateEntry(flags, state);

                for (TristateEntry existing : this.entries) {
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
                if (parse.args.length < 2)
                    return ProcessResult.of(false, Text.of("Must specify flags or an index to remove!"));
                if(this.entries.isEmpty()) return ProcessResult.of(false, "There are no entries to remove!");
                try {
                    int index = Integer.parseInt(parse.args[1]);
                    if (index < 0) index = 0;
                    else if (index >= this.entries.size()) index = this.entries.size() - 1;
                    removeFlagEntry(index);
                    return ProcessResult.of(true, Text.of("Successfully removed flag entry!"));
                } catch (NumberFormatException ignored) {
                    Set<Flag> flags = new HashSet<>();
                    for (int i = 1; i < parse.args.length; i++) {
                        String argument = parse.args[i];
                        Optional<Flag> flagOptional = FlagRegistry.getInstance().getFlag(argument);
                        if (flagOptional.isPresent()) {
                            flags.add(flagOptional.get());
                        } else {
                            return ProcessResult.of(false, Text.of("\"" + argument + "\" is not a valid flag!"));
                        }
                    }
                    TristateEntry entry = null;
                    for (TristateEntry existing : this.entries) {
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
                if (parse.args.length < 2)
                    return ProcessResult.of(false, Text.of("Must specify an index or flags and then a tristate value!"));
                if (this.entries.isEmpty()) return ProcessResult.of(false, "There are no entries to set!");
                try {
                    int index = Integer.parseInt(parse.args[1]);
                    if (index < 0) index = 0;
                    else if (index >= this.entries.size()) index = this.entries.size() - 1;
                    if (parse.args.length < 3)
                        return ProcessResult.of(false, Text.of("Must specify a tristate value!"));
                    String tristateArg = parse.args[2];
                    if (tristateArg.startsWith("=")) tristateArg = tristateArg.substring(1);
                    if (isIn(CLEAR_ALIASES, tristateArg)) {
                        this.removeFlagEntry(index);
                        return ProcessResult.of(true, Text.of("Successfully cleared flag entry!"));
                    } else {
                        Tristate state = Aliases.tristateFrom(tristateArg);
                        if (state == null)
                            return ProcessResult.of(false, Text.of("\"" + tristateArg + "\" is not a valid tristate value!"));
                        this.setFlagEntry(index, state);
                        return ProcessResult.of(true, Text.of("Successfully set flag entry!"));
                    }
                } catch (NumberFormatException ignored) {
                }
                Tristate state = null;
                Set<Flag> flags = new HashSet<>();
                boolean areFlagsSet = false;
                boolean clear = false;
                for (int i = 1; i < parse.args.length; i++) {
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
                if (state == null && !clear) return ProcessResult.of(false, Text.of("Must specify a tristate value!"));
                TristateEntry entry = new TristateEntry(flags, state);

                TristateEntry original = null;
                for (TristateEntry existing : this.entries) {
                    if (existing.set.equals(entry.set)) {
                        original = existing;
                        break;
                    }
                }
                if (clear) {
                    if (original != null) {
                        this.removeFlagEntry(this.entries.indexOf(original));
                        return ProcessResult.of(true, Text.of("Successfully cleared flag entry!"));
                    } else {
                        return ProcessResult.of(false, Text.of("No entry exists with these flags to be cleared!"));
                    }
                } else {
                    if (original != null) this.entries.remove(original);
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
            }
            case "move": {
                if (parse.args.length < 2)
                    return ProcessResult.of(false, Text.of("Must specify flags or an index to move!"));
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
                    TristateEntry entry = null;
                    for (TristateEntry existing : this.entries) {
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
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("add", "set", "remove", "move").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index >= 1) {
                switch (parse.args[0].toLowerCase()) {
                    case "add": {
                        if (parse.current.token.startsWith("=")) {
                            return ImmutableList.of("=allow", "=deny", "=pass").stream()
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        } else {
                            String[] flagArgs = Arrays.copyOfRange(parse.args, 1, parse.args.length);
                            return FlagRegistry.getInstance().getFlagList().stream()
                                    .map(Flag::getName)
                                    .filter(arg -> !isIn(flagArgs, arg))
                                    .filter(new StartsWithPredicate(parse.current.token))
                                    .map(args -> parse.current.prefix + args)
                                    .collect(GuavaCollectors.toImmutableList());
                        }
                    }
                    case "set": {
                        if (parse.current.index == 1) {
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
                        } else if (parse.current.index == 2) try {
                            Integer.parseInt(parse.args[1]);
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
                            String[] flagArgs = Arrays.copyOfRange(parse.args, 1, parse.args.length);
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
                        String[] flagArgs = Arrays.copyOfRange(parse.args, 1, parse.args.length);
                        return FlagRegistry.getInstance().getFlagList().stream()
                                .map(Flag::getName)
                                .filter(arg -> !isIn(flagArgs, arg))
                                .filter(new StartsWithPredicate(parse.current.token))
                                .map(args -> parse.current.prefix + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.AQUA,
                TextActions.suggestCommand("/foxguard md h " + this.name + " add "),
                TextActions.showText(Text.of("Click to add a flag entry")),
                "Flags:"));
        int index = 0;
        for (TristateEntry entry : this.entries) {
            StringBuilder stringBuilder = new StringBuilder();
            entry.set.stream().sorted().forEach(flag -> stringBuilder.append(flag.name).append(" "));
            Text.Builder entryBuilder = Text.builder();
            entryBuilder.append(Text.of("  " + index + ": " + stringBuilder.toString(), TextColors.AQUA, ": "))
                    .append(FGUtil.readableTristateText(entry.tristate))
                    .onHover(TextActions.showText(Text.of("Click to change this flag entry")))
                    .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " set " + (index++) + " "));
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
        Path flagsFile = directory.resolve("flags.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(flagsFile).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(flagsFile, loader);
        root.getNode("flags").setValue(this.entries.stream()
                .map(TristateEntry::serialize)
                .collect(Collectors.toList())
        );
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addFlagEntry(TristateEntry entry) {
        return addFlagEntry(0, entry);
    }

    public boolean addFlagEntry(int index, TristateEntry entry) {
        if (index < 0 || index > this.entries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + this.entries.size());
        for (TristateEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) return false;
        }
        this.entries.add(index, entry);
        this.permCache.clear();
        return true;
    }

    public void setFlagEntry(TristateEntry entry) {
        for (TristateEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) {
                groupEntry.tristate = entry.tristate;
                return;
            }
        }
        this.entries.add(entry);
        this.permCache.clear();
    }

    public void setFlagEntry(int index, TristateEntry entry) {
        TristateEntry original = null;
        for (TristateEntry groupEntry : this.entries) {
            if (groupEntry.set.equals(entry.set)) {
                original = groupEntry;
                break;
            }
        }
        if (original != null) this.entries.remove(original);
        this.entries.add(index, entry);
        this.permCache.clear();
    }

    public void setFlagEntry(int index, Tristate state) {
        if (index < 0 || index >= this.entries.size())
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " Range: 0-" + (this.entries.size() - 1));
        TristateEntry entry = this.entries.get(index);
        entry.tristate = state;
        this.permCache.clear();
    }

    public boolean removeFlagEntry(Set<Flag> flags) {
        TristateEntry toRemove = null;
        for (TristateEntry groupEntry : this.entries) {
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
        TristateEntry toMove = null;
        for (TristateEntry groupEntry : this.entries) {
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
        TristateEntry entry = this.entries.remove(source);
        this.entries.add(destination, entry);
        this.permCache.clear();
    }

    public void load(Path directory) {
        Path flagsFile = directory.resolve("flags.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(flagsFile).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(flagsFile, loader);
        List<Optional<String>> optionalFlagsList = root.getNode("flags").getList(o -> {
            if (o instanceof String) {
                return Optional.of((String) o);
            } else return Optional.empty();
        });
        this.entries.clear();
        optionalFlagsList.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TristateEntry::deserialize)
                .distinct()
                .forEach(this.entries::add);
        this.permCache.clear();
    }

    public static class Factory implements IHandlerFactory {

        public static final String[] ALIASES = {"static", "stub", "blind"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            return new StaticHandler(name, priority);
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            StaticHandler handler = new StaticHandler(name, priority, isEnabled);
            handler.load(directory);
            return handler;
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "static";
        }

        @Override
        public String getPrimaryAlias() {
            return "static";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return ImmutableList.of();
        }
    }
}
