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
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxcore.plugin.util.IWorldBound;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.object.owners.OwnerProviderRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;
import static net.foxdenstudio.sponge.foxguard.plugin.FGManager.SERVER_UUID;

public class CommandDetail extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        } else if (isIn(ALL_ALIASES, key) && !map.containsKey("all")) {
            map.put("all", value);
        }
        return true;
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else {
            String category = parse.args[0];
            FGCat fgCat = FGCat.from(category);
            if (fgCat == null) throw new CommandException(Text.of("\"" + category + "\" is not a valid category!"));

            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(parse.args[1]);

            IFGObject object;
            switch (fgCat) {
                case REGION:
                    object = FGUtil.getRegionFromCommand(source, ownerResult, parse.flags.containsKey("world"), parse.flags.get("world"));
                    if (object instanceof IWorldRegion) fgCat = FGCat.WORLDREGION;
                    break;
                case HANDLER:
                    object = FGUtil.getHandlerFromCommand(ownerResult);
                    if (object instanceof IController) fgCat = FGCat.CONTROLLER;
                    break;
                default:
                    throw new CommandException(Text.of("Something went horribly wrong."));
            }
            UUID owner = object.getOwner();
            String name = object.getName();
            String fullName = name;
            boolean hasOwner = false;
            if (owner != null && owner != FGManager.SERVER_UUID) {
                fullName = owner + ":" + fullName;
                hasOwner = true;
            }

            boolean general = false;
            if (parse.args.length < 3 || parse.args[2].isEmpty() || parse.flags.containsKey("all")) general = true;

            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
            if (general) {
                builder.append(Text.of(TextActions.runCommand("/foxguard det " + fgCat.sName + " " + FGUtil.genWorldFlag(object) + fullName),
                        TextActions.showText(Text.of("View details for " + fgCat.lName + " \"" + name + "\"")),
                        TextColors.GREEN, "------- General -------\n",
                        TextColors.GOLD, "Name: ", TextColors.RESET, name + "\n"));
                if (hasOwner) {
                    OwnerProviderRegistry registry = OwnerProviderRegistry.getInstance();
                    builder.append(Text.builder()
                            .append(Text.of(TextColors.GOLD, "Owner: "))
                            .append(registry.getDisplayText(owner, null, source))
                            .append(Text.NEW_LINE)
                            .onHover(TextActions.showText(registry.getHoverText(owner, null, source)))
                            .build()
                    );
                }
                builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, object.getLongTypeName() + "\n"));
                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Enabled: "))
                        .append(Text.of((object.isEnabled() ? TextColors.GREEN : TextColors.RED), (object.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (object.isEnabled() ? "disable" : "enable") +
                                " " + fgCat.sName + " " + FGUtil.genWorldFlag(object) + fullName))
                        .onHover(TextActions.showText(Text.of("Click to " + (object.isEnabled() ? "disable" : "enable"))))
                        .build());

                if (object instanceof IWorldBound)
                    builder.append(Text.of(TextColors.GOLD, "World: "), Text.of(TextColors.RESET, ((IWorldBound) object).getWorld().getName() + "\n"));

                if (object instanceof IHandler) {
                    builder.append(Text.builder()
                            .append(Text.of(TextColors.GOLD, "Priority: "))
                            .append(Text.of(TextColors.RESET, ((IHandler) object).getPriority() + "\n"))
                            .onClick(TextActions.suggestCommand("/foxguard prio " + fullName + " "))
                            .onHover(TextActions.showText(Text.of("Click to change priority")))
                            .build());
                }

                builder.append(Text.of(TextActions.suggestCommand("/foxguard modify " + fgCat.sName + " " + FGUtil.genWorldFlag(object) + fullName + " "),
                        TextActions.showText(Text.of("Click to modify " + fgCat.lName + " \"" + name + "\"")),
                        TextColors.GREEN, "------- Details -------\n"));
            } else {
                builder.append(Text.of(TextColors.GREEN, "------- Details for " + fgCat.lName + " \"" + name + "\"" +
                        (object instanceof IWorldBound ? (" in World \"" + ((IWorldBound) object).getWorld().getName() + "\"") : "") +
                        " -------\n"));
            }

            try {
                Text objectDetails = object.details(source, parse.args.length < 3 ? "" : parse.args[2]);
                if (objectDetails == null) objectDetails = Text.of();
                builder.append(objectDetails);
            } catch (Exception e) {
                builder.append(Text.of(TextColors.RED, TextStyles.ITALIC, "There was an exception getting details for " + fgCat.lName + " \"" + name + "\"."));
                FoxGuardMain.instance().getLogger().error(
                        fgCat.uName + "\"" + name + "\" of type \"" + object.getLongTypeName() + "\""
                                + (object instanceof IWorldBound ? " in world \"" + ((IWorldBound) object).getWorld().getName() + "\"" : "")
                                + " threw an exception while getting details", e
                );
            }

            if (general) {
                if(object instanceof IHandler){
                    IHandler handler = ((IHandler) object);
                    builder.append(Text.of(TextColors.GREEN, "\n------- Inbound Links -------"));
                    List<IController> controllerList = FGManager.getInstance().getControllers().stream()
                            .filter(controller -> controller.getLinks().contains(handler))
                            .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                            .collect(GuavaCollectors.toImmutableList());
                    List<IRegion> regionList = FGManager.getInstance().getAllRegions().stream()
                            .filter(region -> region.getLinks().contains(handler))
                            .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                            .collect(GuavaCollectors.toImmutableList());
                    if (controllerList.size() == 0 && regionList.size() == 0)
                        builder.append(Text.of(TextStyles.ITALIC, "\nNo inbound links!"));
                    controllerList.forEach(controller -> {
                        builder.append(Text.NEW_LINE);
                        if (source instanceof Player) {
                            List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                            List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                            if (selectedHandlers.contains(controller)) {
                                builder.append(Text.of(TextColors.GRAY, "[h+]"));
                                builder.append(Text.of(TextColors.RED,
                                        TextActions.runCommand("/foxguard s h remove " + controller.getName()),
                                        TextActions.showText(Text.of("Remove from handler state buffer")),
                                        "[h-]"));
                            } else {
                                builder.append(Text.of(TextColors.GREEN,
                                        TextActions.runCommand("/foxguard s h add " + controller.getName()),
                                        TextActions.showText(Text.of("Add to handler state buffer")),
                                        "[h+]"));
                                builder.append(Text.of(TextColors.GRAY, "[h-]"));
                            }
                            if (selectedControllers.contains(controller)) {
                                builder.append(Text.of(TextColors.GRAY, "[c+]"));
                                builder.append(Text.of(TextColors.RED,
                                        TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                        TextActions.showText(Text.of("Remove from controller state buffer")),
                                        "[c-]"));
                            } else {
                                builder.append(Text.of(TextColors.GREEN,
                                        TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                        TextActions.showText(Text.of("Add to controller state buffer")),
                                        "[c+]"));
                                builder.append(Text.of(TextColors.GRAY, "[c-]"));
                            }
                            builder.append(Text.of(" "));
                        }
                        builder.append(Text.of(FGUtil.getColorForObject(controller),
                                TextActions.runCommand("/foxguard det c " + controller.getName()),
                                TextActions.showText(Text.of("View details for controller \"" + controller.getName() + "\"")),
                                controller.getShortTypeName() + " : " + controller.getName()));
                    });

                    regionList.forEach(region -> {
                        builder.append(Text.NEW_LINE);
                        if (source instanceof Player) {
                            List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                            if (selectedRegions.contains(region)) {
                                builder.append(Text.of(TextColors.GRAY, "[+]"));
                                builder.append(Text.of(TextColors.RED,
                                        TextActions.runCommand("/foxguard s r remove " +
                                                FGUtil.genWorldFlag(region) +
                                                region.getName()),
                                        TextActions.showText(Text.of("Remove from state buffer")),
                                        "[-]"));
                            } else {
                                builder.append(Text.of(TextColors.GREEN,
                                        TextActions.runCommand("/foxguard s r add " +
                                                FGUtil.genWorldFlag(region) +
                                                region.getName()),
                                        TextActions.showText(Text.of("Add to state buffer")),
                                        "[+]"));
                                builder.append(Text.of(TextColors.GRAY, "[-]"));
                            }
                            builder.append(Text.of(" "));
                        }
                        builder.append(Text.of(FGUtil.getColorForObject(region),
                                TextActions.runCommand("/foxguard detail region " + FGUtil.genWorldFlag(region) + region.getName()),
                                TextActions.showText(Text.of("View details for region \"" + region.getName() + "\"")),
                                FGUtil.getRegionDisplayName(region, true)
                        ));
                    });
                }
                if (object instanceof ILinkable)
                    outboundLinks(builder, (ILinkable) object, source);
            }


            return CommandResult.empty();
        }
    }

    /*public CommandResult processOld(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
                .flagMapper(MAPPER)
                .leaveFinalAsIs(true)
                .parse();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isIn(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(parse.args[1]);
            String worldName = parse.flags.get("world");

            IRegion region = FGUtil.getRegionFromCommand(source, ownerResult, worldName);

            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
            if (parse.args.length < 3 || parse.args[2].isEmpty() || parse.flags.containsKey("all")) {
                builder.append(Text.of(TextActions.runCommand("/foxguard detail region " + FGUtil.genWorldFlag(region) + region.getName()),
                        TextActions.showText(Text.of("View details for region \"" + region.getName() + "\"")),
                        TextColors.GREEN, "------- General -------\n",
                        TextColors.GOLD, "Name: ", TextColors.RESET, region.getName() + "\n"));
                builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, region.getLongTypeName() + "\n"));
                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Enabled: "))
                        .append(Text.of((region.isEnabled() ? TextColors.GREEN : TextColors.RED), (region.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (region.isEnabled() ? "disable" : "enable") +
                                " r " + FGUtil.genWorldFlag(region) + region.getName()))
                        .onHover(TextActions.showText(Text.of("Click to " + (region.isEnabled() ? "disable" : "enable"))))
                        .build());
                UUID owner = region.getOwner();
                if (owner != null && owner != FGManager.SERVER_UUID) {
                    OwnerProviderRegistry registry = OwnerProviderRegistry.getInstance();
                    builder.append(Text.builder()
                            .append(Text.of(TextColors.GOLD, "Owner: "))
                            .append(registry.getDisplayText(owner, null, source))
                            .append(Text.NEW_LINE)
                            .onHover(TextActions.showText(registry.getHoverText(owner, null, source)))
                            .build()
                    );
                }
                if (region instanceof IWorldRegion)
                    builder.append(Text.of(TextColors.GOLD, "World: "), Text.of(TextColors.RESET, ((IWorldRegion) region).getWorld().getName() + "\n"));
                builder.append(Text.of(TextActions.suggestCommand("/foxguard modify region " + FGUtil.genWorldFlag(region) + region.getName() + " "),
                        TextActions.showText(Text.of("Click to modify region \"" + region.getName() + "\"")),
                        TextColors.GREEN, "------- Details -------\n"));
                try {
                    Text objectDetails = region.details(source, parse.args.length < 3 ? "" : parse.args[2]);
                    if (objectDetails == null) objectDetails = Text.of();
                    builder.append(objectDetails);
                } catch (Exception e) {
                    builder.append(Text.of(TextColors.RED, TextStyles.ITALIC, "There was an error getting details for region \"" + region.getName() + "\"."));
                    FoxGuardMain.instance().getLogger().error(
                            (region instanceof IWorldRegion ? "Worldregion \"" : "Region \"")
                                    + region.getName() + "\" of type \"" + region.getLongTypeName() + "\""
                                    + (region instanceof IWorldRegion ? " in world \"" + ((IWorldRegion) region).getWorld().getName() + "\"" : "")
                                    + " threw an exception while getting details", e
                    );
                }
                outboundLinks(builder, region, source);
            } else {
                builder.append(Text.of(TextColors.GREEN, "------- Details for Region \"" + region.getName() + "\"" +
                        (region instanceof IWorldRegion ? (" in World \"" + ((IWorldRegion) region).getWorld().getName() + "\"") : "") +
                        " -------\n"));
                try {
                    Text objectDetails = region.details(source, parse.args.length < 3 ? "" : parse.args[2]);
                    if (objectDetails == null) objectDetails = Text.of();
                    builder.append(objectDetails);
                } catch (Exception e) {
                    builder.append(Text.of(TextColors.RED, TextStyles.ITALIC, "There was an error getting details for region \"" + region.getName() + "\"."));
                    FoxGuardMain.instance().getLogger().error(
                            (region instanceof IWorldRegion ? "Worldregion \"" : "Region \"")
                                    + region.getName() + "\" of type \"" + region.getLongTypeName() + "\""
                                    + (region instanceof IWorldRegion ? " in world \"" + ((IWorldRegion) region).getWorld().getName() + "\"" : "")
                                    + " threw an exception while getting details", e
                    );
                }
            }
            source.sendMessage(builder.build());
            return CommandResult.empty();
            //--------------------------------------------------------------------------------------------------------------------------------------------------------
            //--------------------------------------------------------------------------------------------------------------------------------------------------------
            //--------------------------------------------------------------------------------------------------------------------------------------------------------
        } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            FGUtil.OwnerResult ownerResult = FGUtil.processUserInput(parse.args[1]);

            Optional<IHandler> handlerOpt = FGManager.getInstance().getHandler(ownerResult.getName(), ownerResult.getOwner());
            if (!handlerOpt.isPresent())
                throw new CommandException(Text.of("No handler exists with the name \"" + ownerResult.getName() + "\""
                        + (ownerResult.getOwner() != SERVER_UUID ? " and owner \"" + ownerResult.getOwnerName() + "\"" : "")
                        + "!"));
            IHandler handler = handlerOpt.get();

            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "\n-----------------------------------------------------\n"));
            if (parse.args.length <= 2 || parse.args[2].isEmpty() || parse.flags.containsKey("all")) {
                builder.append(Text.of(TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                        TextActions.showText(Text.of("View details for handler \"" + handler.getName() + "\"")),
                        TextColors.GREEN, "------- General -------\n",
                        TextColors.GOLD, "Name: ", TextColors.RESET, handler.getName() + "\n"));
                builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, handler.getLongTypeName() + "\n"));

                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Enabled: "))
                        .append(Text.of((handler.isEnabled() ? TextColors.GREEN : TextColors.RED), (handler.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (handler.isEnabled() ? "disable" : "enable") + " h " + handler.getName()))
                        .onHover(TextActions.showText(Text.of("Click to " + (handler.isEnabled() ? "disable" : "enable"))))
                        .build());

                UUID owner = handler.getOwner();
                if (owner != null && owner != FGManager.SERVER_UUID) {
                    OwnerProviderRegistry registry = OwnerProviderRegistry.getInstance();
                    builder.append(Text.builder()
                            .append(Text.of(TextColors.GOLD, "Owner: "))
                            .append(registry.getDisplayText(owner, null, source))
                            .append(Text.NEW_LINE)
                            .onHover(TextActions.showText(registry.getHoverText(owner, null, source)))
                            .build()
                    );
                }

                builder.append(Text.builder()
                        .append(Text.of(TextColors.GOLD, "Priority: "))
                        .append(Text.of(TextColors.RESET, handler.getPriority() + "\n"))
                        .onClick(TextActions.suggestCommand("/foxguard prio " + handler.getName() + " "))
                        .onHover(TextActions.showText(Text.of("Click to change priority")))
                        .build());
                builder.append(Text.of(TextActions.suggestCommand("/foxguard modify handler " + handler.getName() + " "),
                        TextActions.showText(Text.of("Click to modify handler \"" + handler.getName() + "\"")),
                        TextColors.GREEN, "------- Details -------\n"));
                try {
                    Text objectDetails = handler.details(source, parse.args.length < 3 ? "" : parse.args[2]);
                    if (objectDetails == null) objectDetails = Text.of();
                    builder.append(objectDetails);
                } catch (Exception e) {
                    builder.append(Text.of(TextColors.RED, TextStyles.ITALIC, "There was an error getting details for handler \"" + handler.getName() + "\"."));
                    FoxGuardMain.instance().getLogger().error(
                            "Handler \"" + handler.getName() + "\" of type \"" + handler.getLongTypeName() + "\" threw an exception while getting details", e
                    );
                }
                builder.append(Text.of(TextColors.GREEN, "\n------- Inbound Links -------"));
                List<IController> controllerList = FGManager.getInstance().getControllers().stream()
                        .filter(controller -> controller.getLinks().contains(handler))
                        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                        .collect(GuavaCollectors.toImmutableList());
                List<IRegion> regionList = FGManager.getInstance().getAllRegions().stream()
                        .filter(region -> region.getLinks().contains(handler))
                        .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                        .collect(GuavaCollectors.toImmutableList());
                if (controllerList.size() == 0 && regionList.size() == 0)
                    builder.append(Text.of(TextStyles.ITALIC, "\nNo inbound links!"));
                controllerList.forEach(controller -> {
                    builder.append(Text.NEW_LINE);
                    if (source instanceof Player) {
                        List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                        List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                        if (selectedHandlers.contains(controller)) {
                            builder.append(Text.of(TextColors.GRAY, "[h+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s h remove " + controller.getName()),
                                    TextActions.showText(Text.of("Remove from handler state buffer")),
                                    "[h-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s h add " + controller.getName()),
                                    TextActions.showText(Text.of("Add to handler state buffer")),
                                    "[h+]"));
                            builder.append(Text.of(TextColors.GRAY, "[h-]"));
                        }
                        if (selectedControllers.contains(controller)) {
                            builder.append(Text.of(TextColors.GRAY, "[c+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                    TextActions.showText(Text.of("Remove from controller state buffer")),
                                    "[c-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                    TextActions.showText(Text.of("Add to controller state buffer")),
                                    "[c+]"));
                            builder.append(Text.of(TextColors.GRAY, "[c-]"));
                        }
                        builder.append(Text.of(" "));
                    }
                    builder.append(Text.of(FGUtil.getColorForObject(controller),
                            TextActions.runCommand("/foxguard det c " + controller.getName()),
                            TextActions.showText(Text.of("View details for controller \"" + controller.getName() + "\"")),
                            controller.getShortTypeName() + " : " + controller.getName()));
                });

                regionList.forEach(region -> {
                    builder.append(Text.NEW_LINE);
                    if (source instanceof Player) {
                        List<IRegion> selectedRegions = FGUtil.getSelectedRegions(source);
                        if (selectedRegions.contains(region)) {
                            builder.append(Text.of(TextColors.GRAY, "[+]"));
                            builder.append(Text.of(TextColors.RED,
                                    TextActions.runCommand("/foxguard s r remove " +
                                            FGUtil.genWorldFlag(region) +
                                            region.getName()),
                                    TextActions.showText(Text.of("Remove from state buffer")),
                                    "[-]"));
                        } else {
                            builder.append(Text.of(TextColors.GREEN,
                                    TextActions.runCommand("/foxguard s r add " +
                                            FGUtil.genWorldFlag(region) +
                                            region.getName()),
                                    TextActions.showText(Text.of("Add to state buffer")),
                                    "[+]"));
                            builder.append(Text.of(TextColors.GRAY, "[-]"));
                        }
                        builder.append(Text.of(" "));
                    }
                    builder.append(Text.of(FGUtil.getColorForObject(region),
                            TextActions.runCommand("/foxguard detail region " + FGUtil.genWorldFlag(region) + region.getName()),
                            TextActions.showText(Text.of("View details for region \"" + region.getName() + "\"")),
                            FGUtil.getRegionDisplayName(region, true)
                    ));
                });
                if (handler instanceof IController) {
                    outboundLinks(builder, (IController) handler, source);
                }
            } else {
                builder.append(Text.of(TextColors.GREEN, "------- Details for Handler \"" + handler.getName() + "\" -------\n"));
                try {
                    Text objectDetails = handler.details(source, parse.args[2]);
                    if (objectDetails == null) objectDetails = Text.of();
                    builder.append(objectDetails);
                } catch (Exception e) {
                    builder.append(Text.of(TextColors.RED, TextStyles.ITALIC, "There was an error getting details for handler \"" + handler.getName() + "\"."));
                    FoxGuardMain.instance().getLogger().error(
                            "Handler \"" + handler.getName() + "\" of type \"" + handler.getLongTypeName() + "\" threw an exception while getting details", e
                    );
                }
            }
            source.sendMessage(builder.build());
            return CommandResult.empty();
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
    }*/

    private void outboundLinks(Text.Builder builder, ILinkable linkable, CommandSource source) {
        builder.append(Text.of(TextColors.GREEN, "\n------- Outbound Links -------"));
        if (linkable.getLinks().size() == 0)
            builder.append(Text.of(TextStyles.ITALIC, "\nNo outbound links!"));
        Stream<IHandler> handlerStream = linkable.getLinks().stream();
        if (!(linkable instanceof IController))
            handlerStream = handlerStream.sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        handlerStream.forEach(handler -> {
            builder.append(Text.NEW_LINE);
            if (source instanceof Player) {
                List<IHandler> selectedHandlers = FGUtil.getSelectedHandlers(source);
                List<IController> selectedControllers = FGUtil.getSelectedControllers(source);
                if (selectedHandlers.contains(handler)) {
                    builder.append(Text.of(TextColors.GRAY, "[h+]"));
                    builder.append(Text.of(TextColors.RED,
                            TextActions.runCommand("/foxguard s h remove " + handler.getName()),
                            TextActions.showText(Text.of("Remove from handler state buffer")),
                            "[h-]"));
                } else {
                    builder.append(Text.of(TextColors.GREEN,
                            TextActions.runCommand("/foxguard s h add " + handler.getName()),
                            TextActions.showText(Text.of("Add to handler state buffer")),
                            "[h+]"));
                    builder.append(Text.of(TextColors.GRAY, "[h-]"));
                }
                if (handler instanceof IController) {
                    IController controller = ((IController) handler);
                    if (selectedControllers.contains(controller)) {
                        builder.append(Text.of(TextColors.GRAY, "[c+]"));
                        builder.append(Text.of(TextColors.RED,
                                TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                                TextActions.showText(Text.of("Remove from controller state buffer")),
                                "[c-]"));
                    } else {
                        builder.append(Text.of(TextColors.GREEN,
                                TextActions.runCommand("/foxguard s c add " + controller.getName()),
                                TextActions.showText(Text.of("Add to controller state buffer")),
                                "[c+]"));
                        builder.append(Text.of(TextColors.GRAY, "[c-]"));
                    }
                } else {
                    builder.append(Text.of(TextColors.DARK_GRAY, "[c+][c-]"));
                }
                builder.append(Text.of(" "));
            }
            builder.append(Text.of(FGUtil.getColorForObject(handler),
                    TextActions.runCommand("/foxguard det h " + handler.getName()),
                    TextActions.showText(Text.of("View details for " + (handler instanceof IController ? "controller" : "handler") + " \"" + handler.getName() + "\"")),
                    handler.getShortTypeName() + " : " + handler.getName()
            ));
        });
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        if (!testPermission(source)) return ImmutableList.of();
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(2)
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
            else if (parse.current.index == 1) {
                FGUtil.OwnerTabResult result = FGUtil.getOwnerSuggestions(parse.current.token);
                if (result.isComplete()) {
                    return result.getSuggestions().stream()
                            .map(str -> parse.current.prefix)
                            .collect(GuavaCollectors.toImmutableList());
                }

                if (isIn(REGIONS_ALIASES, parse.args[0])) {
                    String worldName = parse.flags.get("world");
                    boolean key = parse.flags.containsKey("world");
                    World world = null;
                    if (source instanceof Locatable) world = ((Locatable) source).getWorld();
                    if (!worldName.isEmpty()) {
                        Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                        if (optWorld.isPresent()) {
                            world = optWorld.get();
                        }
                    }
                    if (key && world != null) {
                        return FGManager.getInstance().getAllRegions(world, result.getOwner()).stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    } else {
                        return FGManager.getInstance().getAllRegionsWithUniqueNames(result.getOwner(), world).stream()
                                .map(IFGObject::getName)
                                .filter(new StartsWithPredicate(result.getToken()))
                                .map(args -> parse.current.prefix + result.getPrefix() + args)
                                .collect(GuavaCollectors.toImmutableList());
                    }
                } else if (isIn(HANDLERS_ALIASES, parse.args[0])) {
                    return FGManager.getInstance().getHandlers().stream()
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(result.getToken()))
                            .map(args -> parse.current.prefix + result.getPrefix() + args)
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

        return source.hasPermission("foxguard.command.info.detail");
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
        return Text.of("detail <region [--w:<worldname>] | handler> <name> [args...]");
    }

    private enum FGCat {
        REGION(REGIONS_ALIASES, "r"),
        WORLDREGION(null, REGION.sName),
        HANDLER(HANDLERS_ALIASES, "h"),
        CONTROLLER(null, HANDLER.sName);

        public final String[] catAliases;
        public final String lName = name().toLowerCase();
        public final String uName = FCCUtil.toCapitalCase(name());
        public final String sName;

        FGCat(String[] catAliases, String sName) {
            this.catAliases = catAliases;
            this.sName = sName;
        }

        public static FGCat from(String category) {
            for (FGCat cat : values()) {
                if (isIn(cat.catAliases, category)) return cat;
            }
            return null;
        }
    }

}
