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

package net.foxdenstudio.sponge.foxguard.plugin.command;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.state.PositionStateField;
import net.foxdenstudio.sponge.foxguard.plugin.FGConfigManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandCreate extends FCCommandBase {

    private static final String[] PRIORITY_ALIASES = {"priority", "prio", "p", "order", "level", "rank"};
    private static final String[] STATE_ALIASES = {"state", "s", "buffer"};

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(PRIORITY_ALIASES, key) && !map.containsKey("priority")) {
            map.put("priority", value);
        } else if (isIn(STATE_ALIASES, key) && !map.containsKey("state")) {
            map.put("state", value);
        }
    };

    @Nonnull
    @Override
    public CommandResult process(@Nonnull CommandSource source, @Nonnull String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(3).flagMapper(MAPPER).parse();

        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isIn(REGIONS_ALIASES, parse.args[0]) || isIn(WORLDREGIONS_ALIASES, parse.args[0])) {
            boolean isWorldRegion = isIn(WORLDREGIONS_ALIASES, parse.args[0]);
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            String worldName = parse.flags.get("world");
            World world = null;
            if (source instanceof Player) world = ((Player) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                } else {
                    if (world == null)
                        throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
                }
            }
            if (isWorldRegion && world == null) throw new CommandException(Text.of("Must specify a world!"));
            if (parse.args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new CommandException(Text.of("Name must be alphanumeric!"));
            if (parse.args[1].matches("^[0-9].*$"))
                throw new CommandException(Text.of("Name can't start with a number!"));
            if (!FGManager.isNameValid(parse.args[1]))
                throw new CommandException(Text.of("You may not use \"" + parse.args[1] + "\" as a name!"));
            int lengthLimit = FGConfigManager.getInstance().getNameLengthLimit();
            if (lengthLimit > 0 && parse.args[1].length() > lengthLimit)
                throw new CommandException(Text.of("Name is too long!"));
            if (isWorldRegion) {
                if (!FGManager.getInstance().isWorldRegionNameAvailable(parse.args[1], world))
                    throw new ArgumentParseException(Text.of("That name is already taken!"), parse.args[1], 1);
            } else {
                if (!FGManager.getInstance().isRegionNameAvailable(parse.args[1]))
                    throw new ArgumentParseException(Text.of("That name is already taken!"), parse.args[1], 1);
            }
            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a type!"));
            IRegion newRegion;
            if (isWorldRegion) {
                List<String> aliases = FGFactoryManager.getInstance().getWorldRegionTypeAliases();
                if (!isIn(aliases.toArray(new String[aliases.size()]), parse.args[2])) {
                    throw new CommandException(Text.of("The type \"" + parse.args[2] + "\" is invalid!"));
                }
                newRegion = FGFactoryManager.getInstance().createWorldRegion(
                        parse.args[1], parse.args[2],
                        parse.args.length < 4 ? "" : parse.args[3],
                        source);
            } else {
                List<String> aliases = FGFactoryManager.getInstance().getRegionTypeAliases();
                if (!isIn(aliases.toArray(new String[aliases.size()]), parse.args[2])) {
                    throw new CommandException(Text.of("The type \"" + parse.args[2] + "\" is invalid!"));
                }
                newRegion = FGFactoryManager.getInstance().createRegion(
                        parse.args[1], parse.args[2],
                        parse.args.length < 4 ? "" : parse.args[3],
                        source);
            }
            if (newRegion == null)
                throw new CommandException(Text.of("Failed to create region! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addRegion(newRegion, world);
            if (!success)
                throw new CommandException(Text.of("There was an error trying to create the " + (isWorldRegion ? "World" : "") + "Region!"));
            FCStateManager.instance().getStateMap().get(source).flush(PositionStateField.ID);
            source.sendMessage(Text.of(TextColors.GREEN, (isWorldRegion ? "Worldr" : "R") + "egion created successfully"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isIn(HANDLERS_ALIASES, parse.args[0]) || isIn(CONTROLLERS_ALIASES, parse.args[0])) {
            boolean isController = isIn(CONTROLLERS_ALIASES, parse.args[0]);
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            if (parse.args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("Name must be alphanumeric!"), parse.args[1], 1);
            if (parse.args[1].matches("^[0-9].*$"))
                throw new ArgumentParseException(Text.of("Name can't start with a number!"), parse.args[1], 1);
            if (!FGManager.isNameValid(parse.args[1]))
                throw new CommandException(Text.of("You may not use \"" + parse.args[1] + "\" as a name!"));
            int lengthLimit = FGConfigManager.getInstance().getNameLengthLimit();
            if (lengthLimit > 0 && parse.args[1].length() > lengthLimit)
                throw new CommandException(Text.of("Name is too long!"));
            int priority = 0;
            try {
                priority = Integer.parseInt(parse.flags.get("priority"));
                if (priority < Integer.MIN_VALUE / 2 + 1) priority = Integer.MIN_VALUE / 2 + 1;
                else if (priority > Integer.MAX_VALUE / 2) priority = Integer.MAX_VALUE / 2;
            } catch (NumberFormatException ignored) {
            }

            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a type!"));
            IHandler newHandler;
            if (isController) {
                List<String> aliases = FGFactoryManager.getInstance().getControllerTypeAliases();
                if (!isIn(aliases.toArray(new String[aliases.size()]), parse.args[2])) {
                    throw new CommandException(Text.of("The type \"" + parse.args[2] + "\" is invalid!"));
                }
                newHandler = FGFactoryManager.getInstance().createController(
                        parse.args[1], parse.args[2], priority,
                        parse.args.length < 4 ? "" : parse.args[3],
                        source);
            } else {
                List<String> aliases = FGFactoryManager.getInstance().getHandlerTypeAliases();
                if (!isIn(aliases.toArray(new String[aliases.size()]), parse.args[2])) {
                    throw new CommandException(Text.of("The type \"" + parse.args[2] + "\" is invalid!"));
                }
                newHandler = FGFactoryManager.getInstance().createHandler(
                        parse.args[1], parse.args[2], priority,
                        parse.args.length < 4 ? "" : parse.args[3],
                        source);
            }
            if (newHandler == null)
                throw new CommandException(Text.of("Failed to create " + (isController ? "controller" : "handler") + "! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addHandler(newHandler);
            if (!success)
                throw new ArgumentParseException(Text.of("That name is already taken!"), parse.args[1], 1);
            source.sendMessage(Text.of(TextColors.GREEN, (isController ? "Controller" : "Handler") + " created successfully!"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
    }

    @Nonnull
    @Override
    public List<String> getSuggestions(@Nonnull CommandSource source, @Nonnull String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(3)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Arrays.asList(FGManager.TYPES).stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 1) {
                if (parse.current.token == null || parse.current.token.isEmpty()) return ImmutableList.of();
                if (parse.current.token.matches("^.*[^0-9a-zA-Z_$].*$")) {
                    source.sendMessage(Text.of(TextColors.RED, "Name must be alphanumeric!"));
                    return ImmutableList.of();
                }
                if (parse.current.token.matches("^[0-9].*$")) {
                    source.sendMessage(Text.of(TextColors.RED, "Name can't start with a number!"));
                    return ImmutableList.of();
                }
                if (!FGManager.isNameValid(parse.current.token)) {
                    source.sendMessage(Text.of(TextColors.RED, "You may not use \"" + parse.current.token + "\" as a name!"));
                    return ImmutableList.of();
                }
                int lengthLimit = FGConfigManager.getInstance().getNameLengthLimit();
                if (lengthLimit > 0 && parse.current.token.length() > lengthLimit) {
                    source.sendMessage(Text.of(TextColors.RED, "Name is too long!"));
                    return ImmutableList.of();
                }

                Tristate available = null;
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    available = Tristate.fromBoolean(FGManager.getInstance().isRegionNameAvailable(parse.current.token));
                } else if (isIn(WORLDREGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    World world = null;
                    if (source instanceof Player) world = ((Player) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (world == null) {
                        available = FGManager.getInstance().isWorldRegionNameAvailable(parse.current.token);
                    } else {
                        available = Tristate.fromBoolean(FGManager.getInstance().isWorldRegionNameAvailable(parse.current.token, world));
                    }
                } else if (isIn(HANDLERS_ALIASES, parse.args[0]) || isIn(CONTROLLERS_ALIASES, parse.args[0])) {
                    available = Tristate.fromBoolean(FGManager.getInstance().gethandler(parse.current.token) == null);
                }
                if (available != null) {
                    switch (available) {
                        case TRUE:
                            source.sendMessage(Text.of(TextColors.GREEN, "Name is available!"));
                            break;
                        case FALSE:
                            source.sendMessage(Text.of(TextColors.RED, "Name is already taken!"));
                            break;
                        case UNDEFINED:
                            source.sendMessage(Text.of(TextColors.YELLOW, "Name might be available. Must specify a world to confirm."));

                    }
                }
            } else if (parse.current.index == 2) {
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryRegionTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(WORLDREGIONS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryWorldRegionTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryHandlerTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(CONTROLLERS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryControllerTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world", "priority").stream()
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.FINAL)) {
            if (isIn(REGIONS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().worldRegionSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().handlerSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(CONTROLLERS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().controllerSuggestions(source, parse.current.token, parse.args[2])
                        .stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(@Nonnull CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.create");
    }

    @Nonnull
    @Override
    public Optional<? extends Text> getShortDescription(@Nonnull CommandSource source) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<? extends Text> getHelp(@Nonnull CommandSource source) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Text getUsage(@Nonnull CommandSource source) {
        return Text.of("create <region [--w:<world>] | handler> <name> [--priority:<num>] <type> [args...]");
    }
}
