/*
 *
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

import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.HANDLERS_ALIASES;
import static net.gravityfox.foxguard.util.Aliases.REGIONS_ALIASES;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class CommandList implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        if (source instanceof Player || (source instanceof ConsoleSource)) {

            String[] args = {};
            if (!arguments.isEmpty()) args = arguments.split(" +");
            int page = 1;

            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (contains(REGIONS_ALIASES, args[0])) {
                List<IRegion> regionList = new LinkedList<>();
                boolean allFlag = true;
                int worldOffset = 0;
                String worldName = "";
                if (args.length > 1) {
                    Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                    if (optWorld != null) {
                        if (!optWorld.isPresent())
                            throw new ArgumentParseException(Texts.of("No world found with that name!"), args[1], 1);
                        FGManager.getInstance().getRegionsListCopy(optWorld.get()).forEach(regionList::add);
                        worldOffset = 1;
                        allFlag = false;
                        worldName = optWorld.get().getName();
                    }
                }
                if (allFlag) {
                    FGManager.getInstance().getRegionsListCopy().forEach(regionList::add);
                }


                TextBuilder output = Texts.builder()
                        .append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"))
                        .append(Texts.of(TextColors.GREEN, "---Regions" + (allFlag ? "" : (" for world: \"" + worldName + "\"")) + "---\n"));
                ListIterator<IRegion> regionListIterator = regionList.listIterator();
                while (regionListIterator.hasNext()) {
                    IRegion region = regionListIterator.next();
                    output.append(Texts.of(FGHelper.getColorForRegion(region), getRegionName(region, allFlag)));
                    if (regionListIterator.hasNext()) output.append(Texts.of("\n"));
                }
                source.sendMessage(output.build());
            } else if (contains(HANDLERS_ALIASES, args[0])) {
                List<IHandler> handlerList = FGManager.getInstance().getHandlerListCopy();

                    /*try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }*/

                TextBuilder output = Texts.builder("---Handlers---\n").color(TextColors.GREEN);
                ListIterator<IHandler> handlerListIterator = handlerList.listIterator();
                while (handlerListIterator.hasNext()) {
                    IHandler handler = handlerListIterator.next();
                    output.append(Texts.of(FGHelper.getColorForHandler(handler),
                            handler.getShortTypeName() + " : " + handler.getName()));
                    if (handlerListIterator.hasNext()) output.append(Texts.of("\n"));
                }
                source.sendMessage(output.build());
            } else {
                throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
            }


            return CommandResult.empty();
        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
    }


    private String getRegionName(IRegion region, boolean dispWorld) {
        return region.getShortTypeName() + " : " + (dispWorld ? region.getWorld().getName() + " : " : "") + region.getName();
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.info.objects.list");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Texts.of("Lists the regions/handlers on this server"));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("list <regions [w:<world>] | handlers>");
    }
}
