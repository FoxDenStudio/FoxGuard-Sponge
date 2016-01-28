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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.state.PositionsStateField;
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
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandCreate implements CommandCallable {

    private static final String[] PRIORITY_ALIASES = {"priority", "prio", "p", "order", "level", "rank"};
    private static final String[] RESERVED = {"all", "state", "full", "everything"};

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(PRIORITY_ALIASES, key) && !map.containsKey("priority")) {
            map.put("priority", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(3).flagMapper(MAPPER).parse();

        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            String worldName = parse.flagmap.get("world");
            World world = null;
            if (source instanceof Player) world = ((Player) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                }
            }
            if (world == null) throw new CommandException(Text.of("Must specify a world!"));
            if (parse.args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("Name must be alphanumeric!"), parse.args[1], 1);
            if (parse.args[1].matches("^[^a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("Name can't start with a number!"), parse.args[1], 1);
            if (parse.args[1].equalsIgnoreCase("all") || parse.args[1].equalsIgnoreCase("state"))
                throw new CommandException(Text.of("You may not use \"" + parse.args[1] + "\" as a name!"));
            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a type!"));
            IRegion newRegion = FGFactoryManager.getInstance().createRegion(
                    parse.args[1], parse.args[2],
                    parse.args.length < 4 ? "" : parse.args[3],
                    source);
            if (newRegion == null)
                throw new CommandException(Text.of("Failed to create Region! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addRegion(world, newRegion);
            if (!success)
                throw new ArgumentParseException(Text.of("That name is already taken!"), parse.args[1], 1);
            FCStateManager.instance().getStateMap().get(source).flush(PositionsStateField.ID);
            source.sendMessage(Text.of(TextColors.GREEN, "Region created successfully"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            if (parse.args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("Name must be alphanumeric!"), parse.args[1], 1);
            if (parse.args[1].matches("^[^a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("Name can't start with a number!"), parse.args[1], 1);
            if (isIn(RESERVED, parse.args[1]))
                throw new CommandException(Text.of("You may not use \"" + parse.args[1] + "\" as a name!"));
            int priority = 0;
            try {
                priority = Integer.parseInt(parse.flagmap.get("priority"));
                if (priority < Integer.MIN_VALUE / 2 + 1) priority = Integer.MIN_VALUE / 2 + 1;
                else if (priority > Integer.MAX_VALUE / 2) priority = Integer.MAX_VALUE / 2;
            } catch (NumberFormatException ignored) {
            }

            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a type!"));
            IHandler newHandler = FGFactoryManager.getInstance().createHandler(
                    parse.args[1], parse.args[2], priority,
                    parse.args.length < 4 ? "" : parse.args[3],
                    source);
            if (newHandler == null)
                throw new CommandException(Text.of("Failed to create Handler! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addHandler(newHandler);
            if (!success)
                throw new ArgumentParseException(Text.of("That name is already taken!"), parse.args[1], 1);
            source.sendMessage(Text.of(TextColors.GREEN, "Handler created successfully!"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .limit(3)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Arrays.asList(FGManager.TYPES).stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 2) {
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryRegionTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGFactoryManager.getInstance().getPrimaryHandlerTypeAliases().stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world", "priority").stream()
                    .filter(new StartsWithPredicate(parse.current.token))
                    .map(args -> parse.current.prefix + args)
                    .collect(GuavaCollectors.toImmutableList());
        else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.LONGFLAGVALUE)) {
            if (isIn(WORLD_ALIASES, parse.current.key))
                return Sponge.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.FINAL)) {
            if (isIn(REGIONS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().regionSuggestions(source, parse.current.token, parse.args[2]);
            } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                return FGFactoryManager.getInstance().handlerSuggestions(source, parse.current.token, parse.args[2]);
            }
        } else if (parse.current.type.equals(AdvCmdParse.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.create");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("create <region [--w:<world>] | handler> <name> [--priority:<num>] <type> [args...]");
    }
}
