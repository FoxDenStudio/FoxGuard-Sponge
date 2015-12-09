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

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.foxcore.commands.FCCommandMainDispatcher;
import net.foxdenstudio.foxcore.commands.util.AdvCmdParse;
import net.foxdenstudio.foxcore.commands.util.ProcessResult;
import net.foxdenstudio.foxcore.util.FCHelper;
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.handlers.IHandler;
import net.foxdenstudio.foxguard.regions.IRegion;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.foxcore.util.Aliases.*;

public class CommandModify implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isAlias(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).limit(2).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();
        if (args.length == 0) {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isAlias(REGIONS_ALIASES, args[0])) {
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
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
            IRegion region = FGManager.getInstance().getRegion(world, args[1]);
            if (region == null)
                throw new CommandException(Texts.of("No Region with name \"" + args[1] + "\"!"));
            ProcessResult result = region.modify(args.length < 3 ? "" : args[2],
                    FCCommandMainDispatcher.getInstance().getStateMap().get(source), source);

            if (result.isSuccess()) {
                if (result.getMessage().isPresent()) {
                    if (!FCHelper.hasColor(result.getMessage().get())) {
                        source.sendMessage(result.getMessage().get().builder().color(TextColors.GREEN).build());
                    } else {
                        source.sendMessage(result.getMessage().get());
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully modified Region!"));
                }
            } else {
                if (result.getMessage().isPresent()) {
                    if (!FCHelper.hasColor(result.getMessage().get())) {
                        source.sendMessage(result.getMessage().get().builder().color(TextColors.RED).build());
                    } else {
                        source.sendMessage(result.getMessage().get());
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Modification Failed for Region!"));
                }
            }
        } else if (isAlias(HANDLERS_ALIASES, args[0])) {
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
            IHandler handler = FGManager.getInstance().gethandler(args[1]);
            if (handler == null)
                throw new CommandException(Texts.of("No Handler with name \"" + args[1] + "\"!"));
            ProcessResult result = handler.modify(args.length < 3 ? "" : args[2],
                    FCCommandMainDispatcher.getInstance().getStateMap().get(source), source);
            if (result.isSuccess()) {
                if (result.getMessage().isPresent()) {
                    if (!FCHelper.hasColor(result.getMessage().get())) {
                        source.sendMessage(result.getMessage().get().builder().color(TextColors.GREEN).build());
                    } else {
                        source.sendMessage(result.getMessage().get());
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully modified Handler!"));
                }
            } else {
                if (result.getMessage().isPresent()) {
                    if (!FCHelper.hasColor(result.getMessage().get())) {
                        source.sendMessage(result.getMessage().get().builder().color(TextColors.RED).build());
                    } else {
                        source.sendMessage(result.getMessage().get());
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Modification Failed for Handler!"));
                }
            }
        } else {
            throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.modify");
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
        return Texts.of("detail <region [--w:<worldname>] | handler> <name> [args...]");
    }
}
