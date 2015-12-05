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

package net.gravityfox.foxguard.commands;

import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.AdvCmdParse;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.factory.FGFactoryManager;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.gravityfox.foxguard.util.Aliases.*;

public class CommandCreate implements CommandCallable {

    private static final String[] PRIORITY_ALIASES = {"priority", "prio", "p", "order", "level", "rank"};

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isAlias(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isAlias(PRIORITY_ALIASES, key) && !map.containsKey("priority")) {
            map.put("priority", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).limit(3).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();
        if (args.length == 0) {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isAlias(REGIONS_ALIASES, args[0])) {
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
            String worldName = parse.getFlagmap().get("world");
            World world = null;
            if (source instanceof Player) world = ((Player) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = FoxGuardMain.getInstance().getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                }
            }
            if (world == null) throw new CommandException(Texts.of("Must specify a world!"));
            if (args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1], 1);
            if (args[1].matches("^[^a-zA-Z_$].*$"))
                throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1], 1);
            if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state"))
                throw new CommandException(Texts.of("You may not use \"" + args[1] + "\" as a name!"));
            if (args.length < 3) throw new CommandException(Texts.of("Must specify a type!"));
            IRegion newRegion = FGFactoryManager.getInstance().createRegion(
                    args[1], args[2],
                    args.length < 4 ? "" : args[3],
                    FGCommandMainDispatcher.getInstance().getStateMap().get(source), world, source);
            if (newRegion == null)
                throw new CommandException(Texts.of("Failed to create Region! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addRegion(world, newRegion);
            if (!success)
                throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1], 1);
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).flush(InternalCommandState.StateField.POSITIONS);
            source.sendMessage(Texts.of(TextColors.GREEN, "Region created successfully"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else if (isAlias(HANDLERS_ALIASES, args[0])) {
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
            if (args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1], 1);
            if (args[1].matches("^[^a-zA-Z_$].*$"))
                throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1], 1);
            if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state") || args[1].equalsIgnoreCase("full"))
                throw new CommandException(Texts.of("You may not use \"" + args[1] + "\" as a name!"));
            int priority = 0;
            try {
                priority = Integer.parseInt(parse.getFlagmap().get("priority"));
            } catch (NumberFormatException ignored) {
            }

            if (args.length < 3) throw new CommandException(Texts.of("Must specify a type!"));
            IHandler newHandler = FGFactoryManager.getInstance().createHandler(
                    args[1], args[2], priority,
                    args.length < 4 ? "" : args[3],
                    FGCommandMainDispatcher.getInstance().getStateMap().get(source), source);
            if (newHandler == null)
                throw new CommandException(Texts.of("Failed to create Handler! Perhaps the type is invalid?"));
            boolean success = FGManager.getInstance().addHandler(newHandler);
            if (!success)
                throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1], 1);
            source.sendMessage(Texts.of(TextColors.GREEN, "Handler created successfully!"));
            return CommandResult.success();
            //----------------------------------------------------------------------------------------------------------------------
        } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
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
        return Texts.of("create <region [--w:<world>] | handler> <name> [--priority:<num>] <type> [args...]");
    }
}
