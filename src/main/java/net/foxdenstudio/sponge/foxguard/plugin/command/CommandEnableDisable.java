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
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.event.factory.FGEventFactory;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.GlobalWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandEnableDisable extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
        return true;
    };

    private final boolean enableState;

    public CommandEnableDisable(boolean enableState) {
        this.enableState = enableState;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();
        if (parse.args.length == 0) {
            if (FGUtil.getSelectedRegions(source).isEmpty() && FGUtil.getSelectedHandlers(source).isEmpty()) {
                source.sendMessage(Text.builder()
                        .append(Text.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else {
                List<IFGObject> objects = new ArrayList<>();
                FGUtil.getSelectedRegions(source).forEach(objects::add);
                FGUtil.getSelectedHandlers(source).forEach(objects::add);
                int successes = 0;
                int failures = 0;
                for (IFGObject object : objects) {
                    if (object instanceof GlobalWorldRegion || object instanceof GlobalHandler || object.isEnabled() == this.enableState)
                        failures++;
                    else {
                        object.setEnabled(this.enableState);
                        successes++;
                    }
                }
                FCStateManager.instance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
                if (successes == 1 && failures == 0) {
                    Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                    source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " object!"));
                    return CommandResult.success();
                } else if (successes > 0) {
                    Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                    source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " objects with "
                            + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                    return CommandResult.builder().successCount(successes).build();
                } else {
                    throw new CommandException(Text.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                            " " + failures + (failures > 1 ? " objects" : " object") + ". Check to make sure you spelled their names correctly and that they are not already "
                            + (this.enableState ? "enabled." : "disabled.")));
                }
            }
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            String worldName = parse.flags.get("world");
            World world = null;
            if (source instanceof Locatable) world = ((Locatable) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                }
            }
            if (world == null) throw new CommandException(Text.of("Must specify a world!"));
            int successes = 0;
            int failures = 0;
            List<IRegion> regions = new ArrayList<>();
            FGUtil.getSelectedRegions(source).forEach(regions::add);
            if (parse.args.length > 1) {
                for (String name : Arrays.copyOfRange(parse.args, 1, parse.args.length)) {
                    IWorldRegion region = FGManager.getInstance().getWorldRegion(world, name).orElse(null);
                    if (region == null) failures++;
                    else {
                        regions.add(region);
                    }
                }
            }
            if (regions.isEmpty()) throw new CommandException(Text.of("Must specify at least one region!"));
            for (IRegion region : regions) {
                if (region instanceof IGlobal || region.isEnabled() == this.enableState) failures++;
                else {
                    region.setEnabled(this.enableState);
                    successes++;
                }
            }
            FCStateManager.instance().getStateMap().get(source).flush(RegionsStateField.ID);
            if (successes == 1 && failures == 0) {
                Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " region!"));
                return CommandResult.success();
            } else if (successes > 0) {
                Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " regions with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                return CommandResult.builder().successCount(successes).build();
            } else {
                throw new CommandException(Text.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                        " " + failures + (failures > 1 ? " regions" : " region") + ". Check to make sure you spelled their names correctly and that they are not already "
                        + (this.enableState ? "enabled." : "disabled.")));
            }

        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            int successes = 0;
            int failures = 0;
            List<IHandler> handlers = new ArrayList<>();
            FGUtil.getSelectedHandlers(source).forEach(handlers::add);
            for (String name : Arrays.copyOfRange(parse.args, 1, parse.args.length)) {
                IHandler handler = FGManager.getInstance().getHandler(name).orElse(null);
                if (handler == null) failures++;
                else {
                    handlers.add(handler);
                }
            }
            if (handlers.isEmpty()) throw new CommandException(Text.of("Must specify at least one handler!"));
            for (IHandler handler : handlers) {
                if (handler instanceof IGlobal || handler.isEnabled() == this.enableState) failures++;
                else {
                    handler.setEnabled(this.enableState);
                    successes++;
                }
            }
            FCStateManager.instance().getStateMap().get(source).flush(HandlersStateField.ID);
            if (successes == 1 && failures == 0) {
                Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " handler!"));
                return CommandResult.success();
            } else if (successes > 0) {
                Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
                source.sendMessage(Text.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " handlers with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                return CommandResult.builder().successCount(successes).build();
            } else {
                throw new CommandException(Text.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                        " " + failures + (failures > 1 ? " handlers" : " handler") + ". Check to make sure you spelled their names correctly and that they are not already "
                        + (this.enableState ? "enabled." : "disabled.")));
            }
        } else throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .flagMapper(MAPPER)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return Stream.of("region", "handler")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            else if (parse.current.index > 0) {
                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    World world = null;
                    if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    String[] existing = Arrays.copyOfRange(parse.args, 1, parse.args.length);
                    if (world == null) return FGManager.getInstance().getRegions().stream()
                            .filter(region -> region.isEnabled() != this.enableState && !(region instanceof IGlobal))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .filter(alias -> !isIn(existing, alias))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                    return FGManager.getInstance().getAllRegions(world).stream()
                            .filter(region -> region.isEnabled() != this.enableState && !(region instanceof IGlobal))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .filter(alias -> !isIn(existing, alias))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    String[] existing = Arrays.copyOfRange(parse.args, 1, parse.args.length);
                    return FGManager.getInstance().getHandlers().stream()
                            .filter(handler -> handler.isEnabled() != this.enableState && !(handler instanceof IGlobal))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .filter(alias -> !isIn(existing, alias))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
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
        return source.hasPermission("foxguard.command.modify.objects.enabledisable");
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
        return Text.of((this.enableState ? "enable" : "disable") + " <region [--w:worldname]> | handler> [names]...");
    }
}
