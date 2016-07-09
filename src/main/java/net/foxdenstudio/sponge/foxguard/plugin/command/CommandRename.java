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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.GlobalWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandRename implements CommandCallable {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("You must specify a name!"));
            IRegion region = null;
            World world = null;
            if (!parse.flags.keySet().contains("world"))
                region = FGManager.getInstance().getRegion(parse.args[1]);
            if (region == null) {
                String worldName = parse.flags.get("world");
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
                if (world == null) throw new CommandException(Text.of("Must specify a world!"));
                region = FGManager.getInstance().getWorldRegion(world, parse.args[1]);
            }
            if (region == null)
                throw new CommandException(Text.of("No region exists with the name \"" + parse.args[1] + "\"!"));
            if (region instanceof GlobalWorldRegion)
                throw new CommandException(Text.of("You may not rename the global region!"));

            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a new name!"));
            if (parse.args[2].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("New name (\"" + parse.args[2] + "\") must be alphanumeric!"), parse.args[2], 1);
            if (parse.args[2].matches("^[0-9].*$"))
                throw new ArgumentParseException(Text.of("New name (\"" + parse.args[2] + "\") can't start with a number!"), parse.args[2], 1);
            if (region.getName().equalsIgnoreCase(parse.args[2]))
                throw new CommandException(Text.of("You cannot rename a region to its own name."));
            if (region instanceof IWorldRegion) {
                if (!FGManager.getInstance().isWorldRegionNameAvailable(parse.args[2], ((IWorldRegion) region).getWorld()))
                    throw new CommandException(Text.of("There is already a region with the name \"" + parse.args[2] + "\"!"));
            } else {
                if (!FGManager.getInstance().isRegionNameAvailable(parse.args[2]))
                    throw new CommandException(Text.of("There is already a region with the name \"" + parse.args[2] + "\"!"));
            }
            String oldName = region.getName();
            FGManager.getInstance().rename(region, parse.args[2]);
            source.sendMessage(Text.of(TextColors.GREEN, "Region \"" + oldName + "\" successfully renamed to \"" + parse.args[2] + "\"!"));
        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("You must specify a name!"));
            IHandler handler = FGManager.getInstance().gethandler(parse.args[1]);
            if (handler == null)
                throw new CommandException(Text.of("No handler exists with the name \"" + parse.args[1] + "\"!"));
            if (handler instanceof GlobalHandler) {
                throw new CommandException(Text.of("You may not rename the global handler!"));
            }
            if (parse.args.length < 3) throw new CommandException(Text.of("Must specify a new name!"));
            if (parse.args[2].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Text.of("New name (\"" + parse.args[2] + "\") must be alphanumeric!"), parse.args[2], 1);
            if (parse.args[2].matches("^[0-9].*$"))
                throw new ArgumentParseException(Text.of("New name (\"" + parse.args[2] + "\") can't start with a number!"), parse.args[2], 1);
            if (handler.getName().equalsIgnoreCase(parse.args[2]))
                throw new CommandException(Text.of("You cannot rename a handler to its own name."));
            if (FGManager.getInstance().gethandler(parse.args[2]) != null)
                throw new CommandException(Text.of("There is already a handler with the name \"" + parse.args[2] + "\"!"));
            String oldName = handler.getName();
            FGManager.getInstance().rename(handler, parse.args[2]);
            source.sendMessage(Text.of(TextColors.GREEN, "Handler \"" + oldName + "\" successfully renamed to \"" + parse.args[2] + "\"!"));
        } else throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Arrays.asList(FGManager.TYPES).stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index == 1) {
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    World world = null;
                    if (source instanceof Player) world = ((Player) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (world == null) return FGManager.getInstance().getRegions().stream()
                            .filter(region -> !(region instanceof GlobalWorldRegion))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                    return FGManager.getInstance().getAllRegions(world).stream()
                            .filter(region -> !(region instanceof GlobalWorldRegion))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers().stream()
                            .filter(region -> !(region instanceof GlobalHandler))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 2) {
                Tristate available = null;
                if (isIn(REGIONS_ALIASES, parse.args[0]) || isIn(WORLDREGIONS_ALIASES, parse.args[0])) {
                    IRegion region = null;
                    World world = null;
                    if (!parse.flags.keySet().contains("world"))
                        region = FGManager.getInstance().getRegion(parse.args[1]);
                    if (region == null) {
                        String worldName = parse.flags.get("world");
                        if (source instanceof Player) world = ((Player) source).getWorld();
                        if (!worldName.isEmpty()) {
                            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                            if (optWorld.isPresent()) {
                                world = optWorld.get();
                            }
                        }
                        if (world == null) return ImmutableList.of();
                        region = FGManager.getInstance().getWorldRegion(world, parse.args[1]);
                    }
                    if (region == null || region instanceof GlobalWorldRegion) return ImmutableList.of();
                    if (region instanceof IWorldRegion) {
                        available = Tristate.fromBoolean(FGManager.getInstance().isWorldRegionNameAvailable(parse.current.token, world));
                    } else {
                        available = Tristate.fromBoolean(FGManager.getInstance().isRegionNameAvailable(parse.current.token));
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
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return ImmutableList.of("world").stream()
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
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.rename");
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
        return Text.of("rename <region [--w:<world>] | handler> <oldname> <newname>");
    }
}
