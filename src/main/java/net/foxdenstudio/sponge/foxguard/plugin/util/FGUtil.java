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

package net.foxdenstudio.sponge.foxguard.plugin.util;

import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.event.factory.FGEventFactory;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.object.owners.OwnerProviderRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.state.ControllersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.foxdenstudio.sponge.foxguard.plugin.FGManager.SERVER_UUID;

public final class FGUtil {

    private static UserStorageService userStorageService;

    public static TextColor getColorForObject(IFGObject object) {
        if (object instanceof GlobalRegion) return TextColors.LIGHT_PURPLE;
        else if (object instanceof IGlobal) return TextColors.YELLOW;
        else if (!object.isEnabled()) return TextColors.GRAY;
        else if (object instanceof IController) return TextColors.GREEN;
        else if ((object instanceof IRegion) && !(object instanceof IWorldRegion)) return TextColors.AQUA;
        else return TextColors.WHITE;
    }

    @SuppressWarnings("unchecked")
    public static List<IRegion> getSelectedRegions(CommandSource source) {
        return ((RegionsStateField) FCStateManager.instance().getStateMap().get(source).getOrCreate(RegionsStateField.ID).get()).getList();
    }

    @SuppressWarnings("unchecked")
    public static List<IHandler> getSelectedHandlers(CommandSource source) {
        return ((HandlersStateField) FCStateManager.instance().getStateMap().get(source).getOrCreate(HandlersStateField.ID).get()).getList();
    }

    @SuppressWarnings("unchecked")
    public static List<IController> getSelectedControllers(CommandSource source) {
        return ((ControllersStateField) FCStateManager.instance().getStateMap().get(source).getOrCreate(ControllersStateField.ID).get()).getList();
    }

    public static String getRegionDisplayName(IRegion region, boolean dispWorld) {
        return region.getShortTypeName() + " : " + (dispWorld && region instanceof IWorldRegion ? ((IWorldRegion) region).getWorld().getName() + " : " : "") + region.getName();
    }

    public static String getLogName(IFGObject object) {
        if (userStorageService == null) userStorageService = FoxGuardMain.instance().getUserStorage();
        UUID owner = object.getOwner();
        boolean isOwned = !owner.equals(SERVER_UUID);
        Optional<User> userOwner = isOwned ? userStorageService.get(owner) : Optional.empty();
        return (userOwner.map(user -> user.getName() + ":").orElse("")) + (isOwned ? owner + ":" : "") + object.getName();
    }

    public static String getCategory(IFGObject object) {
        if (object instanceof IRegion) {
            if (object instanceof IWorldRegion) return "worldregion";
            else return "region";
        } else if (object instanceof IHandler) {
            if (object instanceof IController) return "controller";
            else return "handler";
        } else return "object";
    }

    public static String genWorldFlag(IRegion region) {
        return region instanceof IWorldRegion ? "--w:" + ((IWorldRegion) region).getWorld().getName() + " " : "";
    }

    public static Text readableTristateText(Tristate state) {
        switch (state) {
            case UNDEFINED:
                return Text.of(TextColors.YELLOW, "Pass");
            case TRUE:
                return Text.of(TextColors.GREEN, "Allow");
            case FALSE:
                return Text.of(TextColors.RED, "Deny");
            default:
                return Text.of(TextColors.LIGHT_PURPLE, "Wait wat?");
        }
    }

    public static void markRegionDirty(IRegion region) {
        FGManager.getInstance().markDirty(region, RegionCache.DirtyType.MODIFIED);
        FGStorageManagerNew.getInstance().defaultModifiedMap.put(region, true);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
    }

    public static void markHandlerDirty(IHandler handler) {
        FGStorageManagerNew.getInstance().defaultModifiedMap.put(handler, true);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), handler));
    }

    @NotNull
    public static IRegion getRegionFromCommand(CommandSource source, OwnerResult qualifier, String worldName) throws CommandException {
        String name = qualifier.getName();
        UUID owner = qualifier.getOwner();

        Optional<? extends IRegion> regionOpt;
        regionOpt = FGManager.getInstance().getRegion(name, owner);
        if (!regionOpt.isPresent()) {
            World world = null;
            if (source instanceof Locatable) world = ((Locatable) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                } else {
                    if (world == null)
                        throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
                }
            }
            if (world == null) throw new CommandException(Text.of("Must specify a world!"));
            regionOpt = FGManager.getInstance().getWorldRegion(world, name, owner);
        }

        if (!regionOpt.isPresent())
            throw new CommandException(Text.of("No region exists with the name \"" + name + "\""
                    + (owner != SERVER_UUID ? " and owner \"" + qualifier.getOwnerName() + "\"" : "")
                    + "!"));
        return regionOpt.get();
    }

    public static OwnerResult processUserInput(String input) throws CommandException {
        String[] parts = input.split(":", 3);

        String name = null;
        String ownerQualifier = null;
        String provider = null;
        switch (parts.length) {
            case 1:
                name = parts[0];
                break;
            case 2:
                ownerQualifier = parts[0];
                name = parts[1];
                break;
            case 3:
                provider = parts[0];
                ownerQualifier = parts[1];
                name = parts[2];
                break;
        }

        if (name == null || name.isEmpty())
            throw new CommandException(Text.of("Name must not be... blank?"));

        Optional<UUID> ownerOpt = OwnerProviderRegistry.getInstance().getUUIDForOwner(provider, ownerQualifier);
        if (!ownerOpt.isPresent()) {
            String errorName = (provider != null ? provider + ":" : "") + ownerQualifier;
            throw new CommandException(Text.of("\"" + errorName + "\" is not a valid owner!"));
        }
        return new OwnerResult(name, ownerOpt.get(), ownerQualifier);
    }

    public static class OwnerResult {
        private String name;
        private UUID owner;
        private String ownerName;

        private OwnerResult(String name, UUID owner, String ownerName) {
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
        }

        public String getName() {
            return name;
        }

        public UUID getOwner() {
            return owner;
        }

        public String getOwnerName() {
            return ownerName;
        }
    }
}
