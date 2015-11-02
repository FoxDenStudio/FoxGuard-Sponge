/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class CommandList implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (source instanceof Player || (source instanceof ConsoleSource)) {

            String[] args = {};
            if (!arguments.isEmpty()) args = arguments.split(" ");
            int page = 1;

            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (contains(regionsAliases, args[0])) {
                List<IRegion> regionList = new LinkedList<>();
                boolean allFlag = true;
                int worldOffset = 0;
                String worldName = "";
                if (args.length > 1) {
                    Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                    if (optWorld != null) {
                        if (!optWorld.isPresent())
                            throw new ArgumentParseException(Texts.of("No world found with that name!"), args[1], 1);
                        FoxGuardManager.getInstance().getRegionsListCopy(optWorld.get()).forEach(regionList::add);
                        worldOffset = 1;
                        allFlag = false;
                        worldName = optWorld.get().getName();
                    }
                }
                if (allFlag) {
                    FoxGuardManager.getInstance().getRegionsListCopy().forEach(regionList::add);
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
            } else if (contains(flagSetsAliases, args[0])) {
                List<IFlagSet> flagSetList = FoxGuardManager.getInstance().getFlagSetsListCopy();

                    /*try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }*/

                TextBuilder output = Texts.builder("---FlagSets---\n").color(TextColors.GREEN);
                ListIterator<IFlagSet> flagSetListIterator = flagSetList.listIterator();
                while (flagSetListIterator.hasNext()) {
                    IFlagSet flagSet = flagSetListIterator.next();
                    output.append(Texts.of(FGHelper.getColorForFlagSet(flagSet),
                            flagSet.getType() + " : " + flagSet.getName()));
                    if (flagSetListIterator.hasNext()) output.append(Texts.of("\n"));
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
        return region.getType() + " : " + (dispWorld ? region.getWorld().getName() + " : " : "") + region.getName();
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.info.list");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Texts.of("Lists the regions/flagsets on this server"));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("list (regions [w:<world>] | flagsets) [page]");
    }
}
