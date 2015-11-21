/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/
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

package net.gravityfox.foxguard.commands;

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
import org.spongepowered.api.util.command.dispatcher.SimpleDispatcher;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.api.util.command.CommandMessageFormatting.NEWLINE_TEXT;
import static org.spongepowered.api.util.command.CommandMessageFormatting.SPACE_TEXT;

/**
 * Created by Fox on 11/2/2015.
 * Project: foxguard
 */
public class FGCommandDispatcher implements Dispatcher {

    protected final Disambiguator disambiguator;
    protected final ListMultimap<String, CommandMapping> commands = ArrayListMultimap.create();
    protected String usagePrefix;
    protected Text shortDescription;

    public FGCommandDispatcher(String usagePrefix, String shortDescription, Disambiguator disambiguator) {
        this.usagePrefix = usagePrefix;
        if (shortDescription == null || shortDescription.isEmpty()) {
            this.shortDescription = null;
        } else {
            this.shortDescription = Texts.of(shortDescription);
        }
        this.disambiguator = disambiguator;
    }

    public FGCommandDispatcher(String usagePrefix, String shortDescription) {
        this(usagePrefix, shortDescription, SimpleDispatcher.FIRST_DISAMBIGUATOR);
    }

    public FGCommandDispatcher(String usagePrefix) {
        this(usagePrefix, null, SimpleDispatcher.FIRST_DISAMBIGUATOR);
    }

    public Optional<CommandMapping> register(CommandCallable callable, String... alias) {
        checkNotNull(alias, "alias");
        return register(callable, Arrays.asList(alias));
    }

    public Optional<CommandMapping> register(CommandCallable callable, List<String> aliases) {
        checkNotNull(aliases, "aliases");
        checkNotNull(callable, "callable");

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

    @Override
    public Set<? extends CommandMapping> getCommands() {
        return ImmutableSet.copyOf(this.commands.values());
    }

    @Override
    public Set<String> getPrimaryAliases() {
        Set<String> aliases = this.commands.values().stream().map(CommandMapping::getPrimaryAlias).collect(Collectors.toSet());
        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public Set<String> getAliases() {
        Set<String> aliases = new HashSet<String>();

        for (CommandMapping mapping : this.commands.values()) {
            aliases.addAll(mapping.getAllAliases());
        }

        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public Optional<? extends CommandMapping> get(String alias) {
        return this.get(alias, null);
    }

    public Optional<CommandMapping> get(String alias, CommandSource source) {
        List<CommandMapping> results = this.commands.get(alias.toLowerCase());
        if (results.size() == 1) {
            return Optional.of(results.get(0));
        } else if (results.size() == 0 || source == null) {
            return Optional.empty();
        } else {
            return this.disambiguator.disambiguate(source, alias, results);
        }
    }

    @Override
    public Set<? extends CommandMapping> getAll(String alias) {
        return ImmutableSet.copyOf(this.commands.get(alias));
    }

    @Override
    public Multimap<String, CommandMapping> getAll() {
        return ImmutableMultimap.copyOf(this.commands);
    }

    @Override
    public boolean containsAlias(String alias) {
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
    public CommandResult process(CommandSource source, String inputArguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        if (!inputArguments.isEmpty()) {
            String[] args = inputArguments.split(" +", 2);
            if (args[0].equalsIgnoreCase("help")) {
                //args = inputArguments.split(" +", 2);
                if (args.length > 1) {
                    final Optional<CommandMapping> optCommand = get(args[1], source);
                    if (!optCommand.isPresent()) {
                        source.sendMessage(Texts.of("That command doesn't exist!"));
                        return CommandResult.empty();
                    }
                    CommandCallable command = optCommand.get().getCallable();
                    if (!command.testPermission(source)) {
                        source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to view help for this command!"));
                        return CommandResult.empty();
                    }
                    @SuppressWarnings("unchecked")
                    final Optional<Text> helpText = (Optional<Text>) command.getHelp(source);
                    TextBuilder builder = Texts.builder();
                    if (helpText.isPresent()) builder.append(Texts.of(TextColors.GREEN, "----------"),
                            Texts.of(TextColors.GOLD, "Command \""),
                            Texts.of(TextColors.GOLD, optCommand.get().getPrimaryAlias()),
                            Texts.of(TextColors.GOLD, "\" Help"),
                            Texts.of(TextColors.GREEN, "----------\n"));
                    source.sendMessage(builder.append(helpText.orElse(Texts.of("Usage: " + usagePrefix + " ").builder().append(command.getUsage(source)).build())).build());
                    return CommandResult.empty();
                } else {
                    source.sendMessage(this.getHelp(source).get());
                    return CommandResult.empty();
                }
            } else {
                final Optional<CommandMapping> cmdOptional = get(args[0], source);
                if (!cmdOptional.isPresent())
                    throw new CommandNotFoundException(Texts.of("Command not found!"), args[0]);

                final String arguments = args.length > 1 ? args[1] : "";
                final CommandCallable command = cmdOptional.get().getCallable();
                try {
                    return command.process(source, arguments);
                } catch (CommandNotFoundException e) {
                    throw new CommandException(Texts.of("No such child command: %s" + e.getCommand()));
                } catch (CommandException e) {
                    Text text = e.getText();
                    if (text == null) text = Texts.of("There was an error processing command: " + args[0]);
                    source.sendMessage(text.builder().color(TextColors.RED).build());
                    source.sendMessage(Texts.of("Usage: " + usagePrefix + " ").builder()
                            .append(command.getUsage(source)).color(TextColors.RED).build());
                    return CommandResult.empty();
                }
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
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        final String[] args = arguments.split(" +", 2);
        Optional<CommandMapping> cmdOptional = get(args[0], source);
        if (args.length == 1) {
            return filterCommands(source).stream().filter(new StartsWithPredicate(args[0])).collect(GuavaCollectors.toImmutableList());
        } else if (!cmdOptional.isPresent()) {
            return ImmutableList.of();
        }
        return cmdOptional.get().getCallable().getSuggestions(source, args[1]);
    }

    private Set<String> filterCommands(final CommandSource src) {
        return Multimaps.filterValues(this.commands, input -> input.getCallable().testPermission(src)).keys().elementSet();
    }

    private Set<CommandMapping> filterCommandMappings(final CommandSource src) {
        return new HashSet<>(
                Multimaps.filterValues(this.commands, input -> input.getCallable().testPermission(src)).values());
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
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.ofNullable(shortDescription);
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        if (this.commands.isEmpty()) {
            return Optional.empty();
        }
        TextBuilder build = Texts.of("Available commands:\n").builder();
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

    @Override
    public Text getUsage(CommandSource source) {
        final TextBuilder build = Texts.builder();
        List<String> commands = filterCommands(source).stream()
                .filter(input -> {
                    if (input == null) {
                        return false;
                    }
                    final Optional<CommandMapping> ret = get(input, source);
                    return ret.isPresent() && ret.get().getPrimaryAlias().equals(input);
                })
                .collect(Collectors.toList());

        for (Iterator<String> it = commands.iterator(); it.hasNext(); ) {
            build.append(Texts.of(it.next()));
            if (it.hasNext()) {
                build.append(CommandMessageFormatting.SPACE_TEXT);
                build.append(CommandMessageFormatting.PIPE_TEXT);
                build.append(CommandMessageFormatting.SPACE_TEXT);
            }
        }
        return build.build();
    }


}
