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
import net.foxdenstudio.foxcommon.commands.util.AdvCmdParse;
import net.foxdenstudio.foxcommon.util.Aliases;
import net.foxdenstudio.foxguard.FGManager;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.handlers.IHandler;
import net.foxdenstudio.foxguard.regions.IRegion;
import net.foxdenstudio.foxguard.util.FGHelper;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommandList implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (Aliases.isAlias(Aliases.WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse parse = AdvCmdParse.builder().arguments(arguments).flagMapper(MAPPER).build();
        String[] args = parse.getArgs();

        if (args.length == 0) {
            source.sendMessage(Texts.builder()
                    .append(Texts.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (contains(Aliases.REGIONS_ALIASES, args[0])) {
            List<IRegion> regionList = new LinkedList<>();
            boolean allFlag = true;
            String worldName = parse.getFlagmap().get("world");
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = FoxGuardMain.instance().game().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    FGManager.getInstance().getRegionsListCopy(optWorld.get()).forEach(regionList::add);
                    allFlag = false;
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
                output.append(Texts.of(FGHelper.getColorForRegion(region),
                        TextActions.runCommand("/foxguard detail region --w:" + region.getWorld().getName() + " " + region.getName()),
                        TextActions.showText(Texts.of("View Details")),
                        getRegionName(region, allFlag)));
                if (regionListIterator.hasNext()) output.append(Texts.of("\n"));
            }
            source.sendMessage(output.build());
        } else if (contains(Aliases.HANDLERS_ALIASES, args[0])) {
            List<IHandler> handlerList = FGManager.getInstance().getHandlerListCopy();

                    /*try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }*/

            TextBuilder output = Texts.builder()
                    .append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"))
                    .append(Texts.of(TextColors.GREEN, "---Handlers---\n"));
            ListIterator<IHandler> handlerListIterator = handlerList.listIterator();
            while (handlerListIterator.hasNext()) {
                IHandler handler = handlerListIterator.next();
                output.append(Texts.of(FGHelper.getColorForHandler(handler),
                        TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                        TextActions.showText(Texts.of("View Details")),
                        handler.getShortTypeName() + " : " + handler.getName()));
                if (handlerListIterator.hasNext()) output.append(Texts.of("\n"));
            }
            source.sendMessage(output.build());
        } else {
            throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
        }
        return CommandResult.empty();
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
        return Texts.of("list <regions [--w:<world>] | handlers>");
    }
}
