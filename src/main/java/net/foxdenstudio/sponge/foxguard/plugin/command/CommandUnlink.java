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
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandUnlink extends FCCommandBase {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +", 2);
        if (args.length == 0) {
            if (FGUtil.getSelectedRegions(source).size() == 0 &&
                    FGUtil.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Text.of("You don't have any regions or handlers in your state buffer!"));
            if (FGUtil.getSelectedRegions(source).size() == 0)
                throw new CommandException(Text.of("You don't have any regions in your state buffer!"));
            if (FGUtil.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Text.of("You don't have any handlers in your state buffer!"));
            int[] count = {0};
            FGUtil.getSelectedRegions(source).forEach(
                    region -> FGUtil.getSelectedHandlers(source).stream()
                            .filter(handler -> !(handler instanceof GlobalHandler))
                            .forEach(handler -> count[0] += FGManager.getInstance().unlink(region, handler) ? 1 : 0));
            source.sendMessage(Text.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
            FCStateManager.instance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
            return CommandResult.builder().successCount(count[0]).build();
        } else if (args[0].equals("FULL")) {
            if (FGUtil.getSelectedRegions(source).size() == 0 &&
                    FGUtil.getSelectedHandlers(source).size() == 0)
                throw new CommandException(Text.of("You don't have any regions or handlers in your state buffer!"));
            int[] count = {0};
            FGUtil.getSelectedRegions(source).forEach(
                    region -> {
                        List<IHandler> handlers = new ArrayList<>();
                        region.getHandlers().stream()
                                .filter(handler -> !(handler instanceof GlobalHandler))
                                .forEach(handlers::add);
                        handlers.forEach(handler -> count[0] += FGManager.getInstance().unlink(region, handler) ? 1 : 0);
                    });
            FGUtil.getSelectedHandlers(source).stream()
                    .filter(handler -> !(handler instanceof GlobalHandler)).forEach(
                    handler -> FGManager.getInstance().getAllRegions().forEach(
                            region -> count[0] += FGManager.getInstance().unlink(region, handler) ? 1 : 0));
            source.sendMessage(Text.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
            FCStateManager.instance().getStateMap().get(source).flush(RegionsStateField.ID, HandlersStateField.ID);
            return CommandResult.builder().successCount(count[0]).build();
        } else if (args[0].equals("EVERYTHING")) {
            int[] count = {0};
            FGManager.getInstance().getAllRegions().forEach(
                    region -> {
                        List<IHandler> handlers = new ArrayList<>();
                        region.getHandlers().stream()
                                .filter(handler -> !(handler instanceof GlobalHandler))
                                .forEach(handlers::add);
                        handlers.stream().forEach(handler -> count[0] += FGManager.getInstance().unlink(region, handler) ? 1 : 0);
                    });
            source.sendMessage(Text.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
            return CommandResult.builder().successCount(count[0]).build();
        } else {
            throw new CommandException(Text.of("Not a supported Unlink operation!"));
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.link.unlink");
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
        return Text.of("unlink [FULL | EVERYTHING]");
    }
}
