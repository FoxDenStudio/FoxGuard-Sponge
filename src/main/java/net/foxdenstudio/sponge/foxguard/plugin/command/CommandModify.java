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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
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
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandModify extends FCCommandBase {

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
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a region name!"));
            IRegion region = null;
            if (!parse.flags.keySet().contains("world"))
                region = FGManager.getInstance().getRegion(parse.args[1]);
            if (region == null) {
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
                if (world == null) throw new CommandException(Text.of("Must specify a world!"));
                region = FGManager.getInstance().getWorldRegion(world, parse.args[1]);
            }
            if (region == null)
                throw new ArgumentParseException(Text.of("No region exists with that name!"), parse.args[1], 1);
            ProcessResult result = region.modify(source, parse.args.length < 3 ? "" : parse.args[2]);
            Optional<Text> messageOptional = result.getMessage();
            if (result.isSuccess()) {
                FGUtil.markRegionDirty(region);

                if (messageOptional.isPresent()) {
                    if (!FCPUtil.hasColor(messageOptional.get())) {
                        source.sendMessage(messageOptional.get().toBuilder().color(TextColors.GREEN).build());
                    } else {
                        source.sendMessage(messageOptional.get());
                    }
                } else {
                    source.sendMessage(Text.of(TextColors.GREEN, "Successfully modified region!"));
                }
            } else {
                if (messageOptional.isPresent()) {
                    if (!FCPUtil.hasColor(messageOptional.get())) {
                        source.sendMessage(messageOptional.get().toBuilder().color(TextColors.RED).build());
                    } else {
                        source.sendMessage(messageOptional.get());
                    }
                } else {
                    source.sendMessage(Text.of(TextColors.RED, "Modification failed for region!"));
                }
            }
        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a handler name!"));
            IHandler handler = FGManager.getInstance().gethandler(parse.args[1]);
            if (handler == null)
                throw new CommandException(Text.of("No handler with name \"" + parse.args[1] + "\"!"));
            ProcessResult result = handler.modify(source, parse.args.length < 3 ? "" : parse.args[2]);
            Optional<Text> messageOptional = result.getMessage();
            if (result.isSuccess()) {
                FGUtil.markHandlerDirty(handler);
                if (messageOptional.isPresent()) {
                    if (!FCPUtil.hasColor(messageOptional.get())) {
                        source.sendMessage(messageOptional.get().toBuilder().color(TextColors.GREEN).build());
                    } else {
                        source.sendMessage(messageOptional.get());
                    }
                } else {
                    source.sendMessage(Text.of(TextColors.GREEN, "Successfully modified handler!"));
                }
            } else {
                if (messageOptional.isPresent()) {
                    if (!FCPUtil.hasColor(messageOptional.get())) {
                        source.sendMessage(messageOptional.get().toBuilder().color(TextColors.RED).build());
                    } else {
                        source.sendMessage(messageOptional.get());
                    }
                } else {
                    source.sendMessage(Text.of(TextColors.RED, "Modification failed for handler!"));
                }
            }
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
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
                .leaveFinalAsIs(true)
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
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                    return FGManager.getInstance().getAllRegions(world).stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers().stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
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
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.FINAL)) {
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
                if (world == null) return ImmutableList.of();
                IWorldRegion region = FGManager.getInstance().getWorldRegion(world, parse.args[1]);
                if (region == null) return ImmutableList.of();
                return region.modifySuggestions(source, parse.current.token).stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                if (parse.args.length < 2) return ImmutableList.of();
                IHandler handler = FGManager.getInstance().gethandler(parse.args[1]);
                if (handler == null) return ImmutableList.of();
                return handler.modifySuggestions(source, parse.current.token).stream()
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.modify");
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("modify <region [--w:<worldname>] | handler> <name> [args...]");
    }
}
