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
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.handlers.GlobalHandler;
import net.foxdenstudio.foxguard.state.HandlersStateField;
import net.foxdenstudio.foxguard.state.RegionsStateField;
import net.foxdenstudio.foxguard.util.FGHelper;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
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

import static net.foxdenstudio.foxcore.util.Aliases.WORLD_ALIASES;
import static net.foxdenstudio.foxcore.util.Aliases.isAlias;

public class CommandLink implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> mapper = map -> key -> value -> {
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
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).flagMapper(mapper).build();
        String[] args = parse.getArgs();
        if (args.length == 0) {
            if (FGHelper.getSelectedRegions(source).size() == 0 &&
                    FGHelper.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions or Handlers in your state buffer!"));
            if (FGHelper.getSelectedRegions(source).size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions in your state buffer!"));
            if (FGHelper.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Texts.of("You don't have any Handlers in your state buffer!"));
            int[] successes = {0};
            FGHelper.getSelectedRegions(source).stream().forEach(
                    region -> FGHelper.getSelectedHandlers(source).stream()
                            .filter(handler -> !(handler instanceof GlobalHandler))
                            .forEach(handler -> successes[0] += FGManager.getInstance().link(region, handler) ? 1 : 0));
            source.sendMessage(Texts.of(TextColors.GREEN, "Successfully linked " + successes[0] + "!"));
            FCCommandMainDispatcher.getInstance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
            return CommandResult.builder().successCount(successes[0]).build();
        } else {
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
            if (args.length < 1) throw new CommandException(Texts.of("Must specify items to link!"));
            if (args.length < 2) throw new CommandException(Texts.of("Must specify a Handler!"));
            boolean success = FGManager.getInstance().link(world, args[0], args[1]);
            if (success) {
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully linked!"));
                return CommandResult.success();
            } else {
                source.sendMessage(Texts.of(TextColors.RED, "There was an error linking. Check their names and also make sure they haven't already been linked."));
                return CommandResult.empty();
            }
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.link.add");
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
        return Texts.of("link [ [--w:<worldname>] <region name> <handler name> ]");
    }
}
