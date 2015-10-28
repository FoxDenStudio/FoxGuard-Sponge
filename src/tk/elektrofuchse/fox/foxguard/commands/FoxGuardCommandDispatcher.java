/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package tk.elektrofuchse.fox.foxguard.commands;

import com.google.common.collect.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.dispatcher.Disambiguator;
import org.spongepowered.api.util.command.dispatcher.Dispatcher;
import tk.elektrofuchse.fox.foxguard.commands.util.CallbackHashMap;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;
import static org.spongepowered.api.util.command.CommandMessageFormatting.NEWLINE_TEXT;
import static org.spongepowered.api.util.command.CommandMessageFormatting.SPACE_TEXT;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public final class FoxGuardCommandDispatcher implements Dispatcher {
    
    public static final Disambiguator FIRST_DISAMBIGUATOR = (source, aliasUsed, availableOptions) -> {
        for (CommandMapping mapping : availableOptions) {
            if (mapping.getPrimaryAlias().toLowerCase().equals(aliasUsed.toLowerCase())) {
                return Optional.of(mapping);
            }
        }
        return Optional.of(availableOptions.get(0));
    };

    private final Disambiguator disambiguatorFunc;
    private final ListMultimap<String, CommandMapping> commands = ArrayListMultimap.create();

    private final Map<CommandSource, InternalCommandState> stateMap = new CallbackHashMap<>((o, m) -> {
        if (o instanceof CommandSource) {
            m.put((CommandSource) o, new InternalCommandState());
        }
    });
    private static FoxGuardCommandDispatcher instance;


    public FoxGuardCommandDispatcher() {
        this(FIRST_DISAMBIGUATOR);
        instance = this;
    }

    public FoxGuardCommandDispatcher(Disambiguator disambiguatorFunc) {
        this.disambiguatorFunc = disambiguatorFunc;
    }

    public Optional<CommandMapping> register(CommandCallable callable, String... alias) {
        checkNotNull(alias, "alias");
        return register(callable, Arrays.asList(alias));
    }

    public Optional<CommandMapping> register(CommandCallable callable, List<String> aliases) {
        return register(callable, aliases, Function.identity());
    }

    public synchronized Optional<CommandMapping> register(CommandCallable callable, List<String> aliases,
                                                          Function<List<String>, List<String>> callback) {
        checkNotNull(aliases, "aliases");
        checkNotNull(callable, "callable");
        checkNotNull(callback, "callback");

        // Invoke the callback with the commands that /can/ be registered
        // noinspection ConstantConditions
        aliases = ImmutableList.copyOf(callback.apply(aliases));
        if (!aliases.isEmpty()) {
            String primary = aliases.get(0);
            List<String> secondary = aliases.subList(1, aliases.size());
            CommandMapping mapping = new ImmutableCommandMapping(callable, primary, secondary);

            for (String alias : aliases) {
                this.commands.put(alias.toLowerCase(), mapping);
            }

            return Optional.of(mapping);
        } else {
            return Optional.empty();
        }
    }

    public synchronized Collection<CommandMapping> remove(String alias) {
        return this.commands.removeAll(alias.toLowerCase());
    }

    public synchronized boolean removeAll(Collection<?> aliases) {
        checkNotNull(aliases, "aliases");

        boolean found = false;

        for (Object alias : aliases) {
            if (!this.commands.removeAll(alias.toString().toLowerCase()).isEmpty()) {
                found = true;
            }
        }

        return found;
    }

    public synchronized Optional<CommandMapping> removeMapping(CommandMapping mapping) {
        checkNotNull(mapping, "mapping");

        CommandMapping found = null;

        Iterator<CommandMapping> it = this.commands.values().iterator();
        while (it.hasNext()) {
            CommandMapping current = it.next();
            if (current.equals(mapping)) {
                it.remove();
                found = current;
            }
        }

        return Optional.ofNullable(found);
    }

    public synchronized boolean removeMappings(Collection<?> mappings) {
        checkNotNull(mappings, "mappings");

        boolean found = false;

        Iterator<CommandMapping> it = this.commands.values().iterator();
        while (it.hasNext()) {
            if (mappings.contains(it.next())) {
                it.remove();
                found = true;
            }
        }

        return found;
    }

    @Override
    public synchronized Set<CommandMapping> getCommands() {
        return ImmutableSet.copyOf(this.commands.values());
    }

    @Override
    public synchronized Set<String> getPrimaryAliases() {
        Set<String> aliases = new HashSet<String>();

        for (CommandMapping mapping : this.commands.values()) {
            aliases.add(mapping.getPrimaryAlias());
        }

        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public synchronized Set<String> getAliases() {
        Set<String> aliases = new HashSet<String>();

        for (CommandMapping mapping : this.commands.values()) {
            aliases.addAll(mapping.getAllAliases());
        }

        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public Optional<CommandMapping> get(String alias) {
        return get(alias, null);
    }


    public synchronized Optional<CommandMapping> get(String alias, @Nullable CommandSource source) {
        List<CommandMapping> results = this.commands.get(alias.toLowerCase());
        if (results.size() == 1) {
            return Optional.of(results.get(0));
        } else if (results.size() == 0 || source == null) {
            return Optional.empty();
        } else {
            return this.disambiguatorFunc.disambiguate(source, alias, results);
        }
    }

    @Override
    public synchronized boolean containsAlias(String alias) {
        return this.commands.containsKey(alias.toLowerCase());
    }

    @Override
    public boolean containsMapping(CommandMapping mapping) {
        checkNotNull(mapping, "mapping");

        for (CommandMapping test : this.commands.values()) {
            if (mapping.equals(test)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CommandResult process(CommandSource source, String commandLine) throws CommandException {

        if (!commandLine.isEmpty()) {
            final String[] argSplit = commandLine.split(" ", 2);
            final Optional<CommandMapping> cmdOptional = get(argSplit[0], source);
            if (!cmdOptional.isPresent()) {
                throw new CommandNotFoundException(t("commands.generic.notFound"), argSplit[0]); // TODO: Fix properly to use a SpongeTranslation??
            }
            final String arguments = argSplit.length > 1 ? argSplit[1] : "";
            final CommandCallable spec = cmdOptional.get().getCallable();
            try {
                return spec.process(source, arguments);
            } catch (CommandNotFoundException e) {
                throw new CommandException(t("No such child command: %s", e.getCommand()));
            }
        } else {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource src, final String arguments) throws CommandException {
        final String[] argSplit = arguments.split(" ", 2);
        Optional<CommandMapping> cmdOptional = get(argSplit[0], src);
        if (argSplit.length == 1) {
            return filterCommands(src).stream().filter(new StartsWithPredicate(argSplit[0])).collect(GuavaCollectors.toImmutableList());
        } else if (!cmdOptional.isPresent()) {
            return ImmutableList.of();
        }
        return cmdOptional.get().getCallable().getSuggestions(src, argSplit[1]);
    }

    @Override
    public boolean testPermission(CommandSource source) {
        for (CommandMapping mapping : this.commands.values()) {
            if (mapping.getCallable().testPermission(source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        if (this.commands.isEmpty()) {
            return Optional.empty();
        }
        TextBuilder build = t("Available commands:\n").builder();
        for (Iterator<CommandMapping> it = filterCommandMappings(source).iterator(); it.hasNext(); ) {

            CommandMapping mapping = it.next();
            @SuppressWarnings("unchecked")
            final Optional<Text> description = (Optional<Text>) mapping.getCallable().getShortDescription(source);
            build.append(Texts.builder(mapping.getPrimaryAlias())
                            .color(TextColors.GREEN)
                            .onClick(TextActions.suggestCommand("/" + mapping.getPrimaryAlias())).build(),
                    SPACE_TEXT, description.orElse(mapping.getCallable().getUsage(source)));
            if (it.hasNext()) {
                build.append(NEWLINE_TEXT);
            }
        }
        return Optional.of(build.build());
    }

    private Set<String> filterCommands(final CommandSource src) {
        return Multimaps.filterValues(this.commands, input -> input.getCallable().testPermission(src)).keys().elementSet();
    }

    private Set<CommandMapping> filterCommandMappings(final CommandSource src) {
        return new HashSet<>(
                Multimaps.filterValues(this.commands, input -> input.getCallable().testPermission(src)).values());
    }

    public synchronized int size() {
        return this.commands.size();
    }

    @Override
    public Text getUsage(final CommandSource source) {
        final TextBuilder build = Texts.builder();
        Iterable<String> filteredCommands = filterCommands(source).stream()
                .filter(input -> {
                    if (input == null) {
                        return false;
                    }
                    final Optional<CommandMapping> ret = get(input, source);
                    return ret.isPresent() && ret.get().getPrimaryAlias().equals(input);
                })
                .collect(Collectors.toList());

        for (Iterator<String> it = filteredCommands.iterator(); it.hasNext(); ) {
            build.append(Texts.of(it.next()));
            if (it.hasNext()) {
                build.append(CommandMessageFormatting.SPACE_TEXT);
                build.append(CommandMessageFormatting.PIPE_TEXT);
                build.append(CommandMessageFormatting.SPACE_TEXT);
            }
        }
        return build.build();
    }

    @Override
    public synchronized Set<CommandMapping> getAll(String alias) {
        return ImmutableSet.copyOf(this.commands.get(alias));
    }

    @Override
    public Multimap<String, CommandMapping> getAll() {
        return ImmutableMultimap.copyOf(this.commands);
    }

    public Map<CommandSource, InternalCommandState> getStateMap() {
        return stateMap;
    }

    public static FoxGuardCommandDispatcher getInstance() {
        return instance;
    }
}
