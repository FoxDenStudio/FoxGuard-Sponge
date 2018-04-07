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
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class CommandLink extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
        return true;
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        if (parse.args.length == 0) {
            if (FGUtil.getSelectedRegions(source).size() == 0 &&
                    FGUtil.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Text.of("You don't have any regions or handlers in your state buffer!"));
            if (FGUtil.getSelectedRegions(source).size() == 0)
                throw new CommandException(Text.of("You don't have any regions in your state buffer!"));
            if (FGUtil.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Text.of("You don't have any handlers in your state buffer!"));
            int[] successes = {0};
            FGUtil.getSelectedRegions(source).forEach(
                    region -> FGUtil.getSelectedHandlers(source).stream()
                            .filter(handler -> !(handler instanceof GlobalHandler))
                            .forEach(handler -> successes[0] += FGManager.getInstance().link(region, handler) ? 1 : 0));
            source.sendMessage(Text.of(TextColors.GREEN, "Successfully formed " + successes[0] + " links!"));
            FCStateManager.instance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
            return CommandResult.builder().successCount(successes[0]).build();
        } else {
            FGUtil.OwnerResult regionOwnerResult = FGUtil.processUserInput(parse.args[0]);
            IRegion region = FGUtil.getRegionFromCommand(source, regionOwnerResult, parse.flags.containsKey("world"), parse.flags.get("world"));

            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a handler!"));
            FGUtil.OwnerResult handlerOwnerResult = FGUtil.processUserInput(parse.args[1]);
            IHandler handler = FGUtil.getHandlerFromCommand(handlerOwnerResult);

            if (region.getLinks().contains(handler))
                throw new CommandException(Text.of("Already linked!"));
            boolean success = FGManager.getInstance().link(region, handler);
            if (success) {
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully linked!"));
                return CommandResult.success();
            } else {
                source.sendMessage(Text.of(TextColors.RED, "There was an error while trying to link."));
                return CommandResult.empty();
            }
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                FGUtil.OwnerTabResult result = FGUtil.getOwnerSuggestions(parse.current.token);
                if (result.isComplete()) {
                    return result.getSuggestions().stream()
                            .map(str -> parse.current.prefix + str)
                            .collect(GuavaCollectors.toImmutableList());
                }

                String worldName = parse.flags.get("world");
                boolean key = parse.flags.containsKey("world");
                World world = null;
                if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                if (!worldName.isEmpty()) {
                    Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                    if (optWorld.isPresent()) {
                        world = optWorld.get();
                    }
                }
                if (key && world != null) {
                    return FGManager.getInstance().getAllRegions(world, result.getOwner()).stream()
                            .map(IGuardObject::getName)
                            .filter(new StartsWithPredicate(result.getToken()))
                            .map(args -> parse.current.prefix + result.getPrefix() + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else {
                    return FGManager.getInstance().getAllRegionsWithUniqueNames(result.getOwner(), world).stream()
                            .map(IGuardObject::getName)
                            .filter(new StartsWithPredicate(result.getToken()))
                            .map(args -> parse.current.prefix + result.getPrefix() + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index == 1) {
                FGUtil.OwnerTabResult tabResult = FGUtil.getOwnerSuggestions(parse.current.token);
                if (tabResult.isComplete()) {
                    return tabResult.getSuggestions().stream()
                            .map(str -> parse.current.prefix + str)
                            .collect(GuavaCollectors.toImmutableList());
                }

                IRegion region = null;
                try {
                    FGUtil.OwnerResult regionOwnerResult = FGUtil.processUserInput(parse.args[0]);
                    region = FGUtil.getRegionFromCommand(source, regionOwnerResult, parse.flags.containsKey("world"), parse.flags.get("world"));
                } catch (CommandException ignored) {
                }
                if (region != null) {
                    IRegion finalRegion = region;
                    return FGManager.getInstance().getHandlers(tabResult.getOwner()).stream()
                            .filter(handler -> !finalRegion.getLinks().contains(handler) && !(handler instanceof IGlobal))
                            .map(IGuardObject::getName)
                            .filter(new StartsWithPredicate(tabResult.getToken()))
                            .map(args -> parse.current.prefix + tabResult.getPrefix() + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
                return FGManager.getInstance().getHandlers(tabResult.getOwner()).stream()
                        .filter(handler -> !(handler instanceof IGlobal))
                        .map(IGuardObject::getName)
                        .filter(new StartsWithPredicate(tabResult.getToken()))
                        .map(args -> parse.current.prefix + tabResult.getPrefix() + args)
                        .collect(GuavaCollectors.toImmutableList());

            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.LONGFLAGKEY))
            return Stream.of("world")
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
        return source.hasPermission("foxguard.command.modify.link.link");
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
        return Text.of("link [ [--w:<worldname>] <region name> <handler name> ]");
    }
}
