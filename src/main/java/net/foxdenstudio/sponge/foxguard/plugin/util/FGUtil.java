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

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.util.IWorldBound;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.event.factory.FGEventFactory;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.object.owners.IOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.owners.OwnerProviderRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.state.ControllersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        boolean isOwned = !owner.equals(FGManager.SERVER_UUID);
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

    public static String genWorldFlag(IFGObject object) {
        return object instanceof IWorldBound ? "--w:" + ((IWorldBound) object).getWorld().getName() + " " : "";
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

    public static void markDirty(IFGObject object){
        if(object instanceof IRegion){
            FGManager.getInstance().markDirty(((IRegion) object), RegionCache.DirtyType.MODIFIED);
        }
        FGStorageManagerNew.getInstance().defaultModifiedMap.put(object, true);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), object));
    }

    @Nonnull
    public static IHandler getHandlerFromCommand(OwnerResult qualifier) throws CommandException {
        String name = qualifier.getName();
        UUID owner = qualifier.getOwner();
        Optional<IHandler> handlerOpt;

        handlerOpt = FGManager.getInstance().getHandler(name, owner);

        if (!handlerOpt.isPresent()) {
            StringBuilder builder = new StringBuilder();
            builder.append("No handler exists with the name \"").append(name).append("\"");
            if (owner != FGManager.SERVER_UUID) {
                builder.append(" and owner \"").append(qualifier.getOwnerName()).append("\"");
            }
            builder.append("!");

            throw new CommandException(Text.of(builder.toString()));
        }
        return handlerOpt.get();
    }

    @Nonnull
    public static IRegion getRegionFromCommand(CommandSource source, OwnerResult qualifier, boolean worldFlag, @Nullable String worldName) throws CommandException {
        String name = qualifier.getName();
        UUID owner = qualifier.getOwner();

        IRegion returnRegion = null;
        /*regionOpt = FGManager.getInstance().getRegion(name, owner);
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
        }*/

        Set<IRegion> regions = FGManager.getInstance().getAllRegions(qualifier.getName(), qualifier.getOwner());
        World world = null;
        if (source instanceof Locatable) world = ((Locatable) source).getWorld();
        if (worldFlag) {
            if (worldName != null && !worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                } else {
                    throw new CommandException(Text.of("No world exists with name \"" + worldName + "\"!"));
                }
            }
            if (world == null)
                throw new CommandException(Text.of("Must specify a world with the world flag!"));
            Optional<IWorldRegion> regionOpt = FGManager.getInstance().getWorldRegion(world, qualifier.getName(), qualifier.getOwner());
            if (regionOpt.isPresent()) {
                returnRegion = regionOpt.get();
            }
        } else {
            if (regions.size() == 1) {
                returnRegion = regions.iterator().next();
            } else if (regions.size() > 1) {
                for (IRegion region : regions) {
                    if (region instanceof IWorldRegion) {
                        if (world == null) continue;
                        if (((IWorldRegion) region).getWorld() == world) returnRegion = region;
                    } else {
                        returnRegion = region;
                        break;
                    }
                }
                if (returnRegion == null && world == null)
                    throw new CommandException(Text.of("Multiple regions exist !"));
            }
        }
        if (returnRegion == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("No region exists with the name \"").append(name).append("\"");
            if (owner != FGManager.SERVER_UUID) {
                builder.append(" and owner \"").append(qualifier.getOwnerName()).append("\"");
            }
            if (world != null && regions.size() > 0) {
                builder.append(" in world \"").append(world.getName()).append("\"");
            }
            builder.append("!");

            throw new CommandException(Text.of(builder.toString()));
        }
        return returnRegion;
    }


    public static OwnerResult processUserInput(String input) throws CommandException {
        if (input.startsWith(":")) input = input.substring(1);
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

        if (name == null)
            throw new CommandException(Text.of("Name must not be... null?"));
        else if (name.isEmpty())
            throw new CommandException(Text.of("Name must not be empty!"));

        Optional<UUID> ownerOpt = OwnerProviderRegistry.getInstance().getUUIDForOwner(provider, ownerQualifier);
        if (!ownerOpt.isPresent()) {
            String errorName = (provider != null ? provider + ":" : "") + ownerQualifier;
            throw new CommandException(Text.of("\"" + errorName + "\" is not a valid owner!"));
        }
        return new OwnerResult(name, ownerOpt.get(), ownerQualifier);
    }

    public static OwnerTabResult getOwnerSuggestions(String input) {
        boolean prefixed = false;
        if (input.startsWith(":")) {
            input = input.substring(1);
            prefixed = true;
        }
        String[] parts = input.split(":", 3);
        if (parts.length == 1) {
            if (prefixed) {
                List<String> list = OwnerProviderRegistry.getInstance().getProviders().stream()
                        .map(IOwnerProvider::getPrimaryAlias)
                        .filter(str -> str != null && !str.isEmpty())
                        .filter(new StartsWithPredicate(parts[0]))
                        .map(str -> ":" + str)
                        .collect(GuavaCollectors.toImmutableList());
                if (list.size() == 1) {
                    list = ImmutableList.of(list.get(0).substring(1) + ":");
                }
                return new OwnerTabResult(list);
            } else {
                return new OwnerTabResult("", parts[0], FGManager.SERVER_UUID);
            }
        } else if (parts.length == 2) {
            OwnerProviderRegistry registry = OwnerProviderRegistry.getInstance();
            Optional<IOwnerProvider> providerOpt = registry.getProvider(parts[0]);
            if (providerOpt.isPresent()) {
                IOwnerProvider provider = providerOpt.get();
                List<String> list = provider.getOwnerKeywords().stream()
                        .filter(str -> str != null && !str.isEmpty())
                        .filter(new StartsWithPredicate(parts[1]))
                        .map(str -> parts[0] + ":" + str)
                        .collect(GuavaCollectors.toImmutableList());
                int size = list.size();
                if (size == 0) {
                    if (prefixed) {
                        list = ImmutableList.of(parts[0] + ":" + parts[1]);
                    }
                } else if (size == 1) {
                    ImmutableList.of(list.get(0) + ":");
                }
                return new OwnerTabResult(list);
            } else {
                Optional<UUID> ownerOpt = registry.getUUIDForOwner(null, parts[0]);
                return ownerOpt.map(uuid -> new OwnerTabResult(parts[0] + ":", parts[1], uuid))
                        .orElseGet(OwnerTabResult::new);

            }
        } else if (parts.length == 3) {
            Optional<IOwnerProvider> providerOpt = OwnerProviderRegistry.getInstance().getProvider(parts[0]);
            if (providerOpt.isPresent()) {
                IOwnerProvider provider = providerOpt.get();
                Optional<UUID> ownerOpt = provider.getOwnerUUID(parts[1]);
                return ownerOpt.map(uuid -> new OwnerTabResult(parts[0] + ":" + parts[1] + ":", parts[2], uuid))
                        .orElseGet(OwnerTabResult::new);
            }

        }
        return new OwnerTabResult();
    }

    public static class OwnerTabResult {
        private boolean complete;
        private List<String> suggestions;
        private String prefix;
        private String token;
        private UUID owner;

        public OwnerTabResult() {
            this.complete = true;
            this.suggestions = ImmutableList.of();
        }

        public OwnerTabResult(List<String> suggestions) {
            this.complete = true;
            this.suggestions = suggestions;
        }

        public OwnerTabResult(String prefix, String token, UUID owner) {
            this.complete = false;
            this.prefix = prefix;
            this.token = token;
            this.owner = owner;
        }

        public boolean isComplete() {
            return complete;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getToken() {
            return token;
        }

        public UUID getOwner() {
            return owner;
        }
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
