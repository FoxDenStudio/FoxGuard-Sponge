/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/20/2015.
 * Project: foxguard
 */
public class CommandState implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        TextBuilder output = Texts.builder().append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"));
        int flag = 0;
        if (!FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.isEmpty()) {
            output.append(Texts.of(TextColors.GREEN, "Regions: "));
            Iterator<IRegion> regionIterator = FGCommandMainDispatcher.getInstance().getStateMap()
                    .get(source).selectedRegions.iterator();
            int index = 1;
            while (regionIterator.hasNext()) {
                IRegion region = regionIterator.next();
                output.append(Texts.of(FGHelper.getColorForRegion(region),
                        "\n  " + (index++) + ": " + region.getShortTypeName() + " : " + region.getWorld().getName() + " : " + region.getName()));
            }
            output.append(Texts.of("\n"));
            flag++;
        }
        if (!FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedHandlers.isEmpty()) {
            if (flag != 0) output.append(Texts.of("\n"));
            output.append(Texts.of(TextColors.GREEN, "Handlers: "));
            Iterator<IHandler> handlerIterator = FGCommandMainDispatcher.getInstance().getStateMap()
                    .get(source).selectedHandlers.iterator();
            int index = 1;
            while (handlerIterator.hasNext()) {
                IHandler handler = handlerIterator.next();
                output.append(Texts.of(FGHelper.getColorForHandler(handler),
                        "\n  " + (index++) + ": " + handler.getShortTypeName() + " : " + handler.getName()));
            }
            output.append(Texts.of("\n"));
            flag++;
        }
        if (!FGCommandMainDispatcher.getInstance().getStateMap().get(source).positions.isEmpty()) {
            if (flag != 0) output.append(Texts.of("\n"));
            output.append(Texts.of(TextColors.GREEN, "Positions:"));
            output.append(Texts.of(TextColors.RESET));
            int index = 1;
            for (Vector3i pos : FGCommandMainDispatcher.getInstance().getStateMap().get(source).positions) {
                output.append(
                        Texts.of("\n  " + (index++) + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                );
            }
            flag++;
        }
        if (flag == 0) source.sendMessage(Texts.of("Your current state buffer is clear!"));
        else source.sendMessage(output.build());

        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.state.state");
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
        return Texts.of("state");
    }
}
