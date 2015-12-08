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

package net.foxdenstudio.foxguard.commands;

import net.foxdenstudio.foxcommon.commands.FCCommandMainDispatcher;
import net.foxdenstudio.foxcommon.commands.util.AdvCmdParse;
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.IFGObject;
import net.foxdenstudio.foxguard.handlers.GlobalHandler;
import net.foxdenstudio.foxguard.handlers.IHandler;
import net.foxdenstudio.foxguard.regions.GlobalRegion;
import net.foxdenstudio.foxguard.regions.IRegion;
import net.foxdenstudio.foxguard.state.HandlersStateField;
import net.foxdenstudio.foxguard.state.RegionsStateField;
import net.foxdenstudio.foxguard.util.FGHelper;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.foxcommon.util.Aliases.*;

public class CommandEnableDisable implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isAlias(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    private final boolean enableState;

    public CommandEnableDisable(boolean enableState) {
        this.enableState = enableState;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();
        if (args.length == 0) {
            if (FGHelper.getSelectedRegions(source).isEmpty() && FGHelper.getSelectedHandlers(source).isEmpty()) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else {
                List<IFGObject> objects = new LinkedList<>();
                FGHelper.getSelectedRegions(source).stream().forEach(objects::add);
                FGHelper.getSelectedHandlers(source).stream().forEach(objects::add);
                int successes = 0;
                int failures = 0;
                for (IFGObject object : objects) {
                    if (object instanceof GlobalRegion || object instanceof GlobalHandler || object.isEnabled() == this.enableState)
                        failures++;
                    else {
                        object.setIsEnabled(this.enableState);
                        successes++;
                    }
                }
                FCCommandMainDispatcher.getInstance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
                if (successes == 1 && failures == 0) {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " object!"));
                    return CommandResult.success();
                } else if (successes > 0) {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " objects with "
                            + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                    return CommandResult.builder().successCount(successes).build();
                } else {
                    throw new CommandException(Texts.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                            " " + failures + (failures > 1 ? " objects" : " object") + ". Check to make sure you spelled their names correctly and that they are not already "
                            + (this.enableState ? "enabled." : "disabled.")));
                }
            }
        } else if (isAlias(REGIONS_ALIASES, args[0])) {
            String worldName = parse.getFlagmap().get("world");
            World world = null;
            if (source instanceof Player) world = ((Player) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = FoxGuardMain.instance().game().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                }
            }
            if (world == null) throw new CommandException(Texts.of("Must specify a world!"));
            int successes = 0;
            int failures = 0;
            List<IRegion> regions = new LinkedList<>();
            FGHelper.getSelectedRegions(source).stream().forEach(regions::add);
            if (args.length > 1) {
                for (String name : Arrays.copyOfRange(args, 1, args.length)) {
                    IRegion region = FGManager.getInstance().getRegion(world, name);
                    if (region == null) failures++;
                    else {
                        regions.add(region);
                    }
                }
            }
            if (regions.isEmpty()) throw new CommandException(Texts.of("Must specify at least one Region!"));
            for (IRegion region : regions) {
                if (region instanceof GlobalRegion || region.isEnabled() == this.enableState) failures++;
                else {
                    region.setIsEnabled(this.enableState);
                    successes++;
                }
            }
            FCCommandMainDispatcher.getInstance().getStateMap().get(source).flush(RegionsStateField.ID);
            if (successes == 1 && failures == 0) {
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " region!"));
                return CommandResult.success();
            } else if (successes > 0) {
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " regions with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                return CommandResult.builder().successCount(successes).build();
            } else {
                throw new CommandException(Texts.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                        " " + failures + (failures > 1 ? " regions" : " region") + ". Check to make sure you spelled their names correctly and that they are not already "
                        + (this.enableState ? "enabled." : "disabled.")));
            }

        } else if (isAlias(HANDLERS_ALIASES, args[0])) {
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
            int successes = 0;
            int failures = 0;
            List<IHandler> handlers = new LinkedList<>();
            FGHelper.getSelectedHandlers(source).stream().forEach(handlers::add);
            for (String name : Arrays.copyOfRange(args, 1, args.length)) {
                IHandler handler = FGManager.getInstance().gethandler(name);
                if (handler == null) failures++;
                else {
                    handlers.add(handler);
                }
            }
            if (handlers.isEmpty()) throw new CommandException(Texts.of("Must specify at least one Handler!"));
            for (IHandler handler : handlers) {
                if (handler instanceof GlobalRegion || handler.isEnabled() == this.enableState) failures++;
                else {
                    handler.setIsEnabled(this.enableState);
                    successes++;
                }
            }
            FCCommandMainDispatcher.getInstance().getStateMap().get(source).flush(HandlersStateField.ID);
            if (successes == 1 && failures == 0) {
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " handler!"));
                return CommandResult.success();
            } else if (successes > 0) {
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " handlers with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
                return CommandResult.builder().successCount(successes).build();
            } else {
                throw new CommandException(Texts.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                        " " + failures + (failures > 1 ? " handlers" : " handler") + ". Check to make sure you spelled their names correctly and that they are not already "
                        + (this.enableState ? "enabled." : "disabled.")));
            }
        } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.enabledisable");
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
        if (source instanceof Player)
            return Texts.of((this.enableState ? "enable" : "disable") + " <region [--w:<worldname>] | handler> [names]...");
        else return Texts.of((this.enableState ? "enable" : "disable") + " <region <worldname> | handler> [names]...");
    }
}
