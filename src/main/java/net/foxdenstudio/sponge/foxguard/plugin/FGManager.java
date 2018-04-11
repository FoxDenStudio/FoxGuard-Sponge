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

package net.foxdenstudio.sponge.foxguard.plugin;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.event.factory.FGEventFactory;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.ServerOwner;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.GlobalWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;
import net.foxdenstudio.sponge.foxguard.plugin.util.RegionCache;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FGManager {

    public static final ServerOwner SERVER_OWNER = ServerOwner.SERVER;
    public static final String[] ILLEGAL_NAMES = {"all", "state", "full", "everything", "users", "owners"};

    private static FGManager instance;
    private final Map<World, Multimap<IOwner, IWorldRegion>> worldRegions;
    private final Multimap<IOwner, IRegion> regions;
    private final Multimap<IOwner, IHandler> handlers;
    private final GlobalRegion globalRegion;
    private final GlobalHandler globalHandler;

    private final RegionCache regionCache;

    private FGManager() {
        instance = this;
        worldRegions = new CacheMap<>((key, map) -> {
            if (key instanceof World) {
                Multimap<IOwner, IWorldRegion> uuidMap = HashMultimap.create();
                map.put((World) key, uuidMap);
                return uuidMap;
            } else return null;
        });
        regions = HashMultimap.create();
        handlers = HashMultimap.create();
        globalRegion = new GlobalRegion();
        globalHandler = new GlobalHandler();
        regions.put(SERVER_OWNER, globalRegion);
        handlers.put(SERVER_OWNER, globalHandler);
        globalRegion.addLink(globalHandler);

        this.regionCache = new RegionCache(regions, worldRegions);
    }

    public static FGManager getInstance() {
        return instance;
    }

    public static void init() {
        if (instance == null) instance = new FGManager();
        if (instance.regions.isEmpty()) instance.regions.put(SERVER_OWNER, instance.globalRegion);
        if (instance.handlers.isEmpty()) instance.handlers.put(SERVER_OWNER, instance.globalHandler);
    }

    public static boolean isNameValid(String name) {
        return !name.isEmpty() &&
                !name.matches("^.*[ :.=;\"\'\\\\/{}()\\[\\]<>#@|?*].*$") &&
                !Aliases.isIn(FGStorageManagerNew.FS_ILLEGAL_NAMES, name) &&
                !Aliases.isIn(ILLEGAL_NAMES, name);
    }

    public boolean addHandler(IHandler handler) {
        IOwner owner = handler.getOwner();
        if (owner == null) owner = SERVER_OWNER;
        return addHandler(handler, owner);
    }

    public boolean addHandler(IHandler handler, IOwner owner) {
        if (handler == null
                || !isNameValid(handler.getName())
                || !isHandlerNameAvailable(handler.getName(), owner)
                || this.handlers.containsValue(handler))
            return false;
        handler.setOwner(owner);
        handlers.put(owner, handler);
        FGStorageManagerNew.getInstance().addObject(handler);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), handler));
        return true;
    }

    public boolean addRegion(IRegion region) {
        IOwner owner = region.getOwner();
        if (owner == null) owner = SERVER_OWNER;
        return addRegion(region, owner);
    }

    public boolean addRegion(IRegion region, @Nonnull IOwner owner) {
        checkNotNull(owner);
        if (region == null
                || !isNameValid(region.getName())
                || !isRegionNameAvailable(region.getName(), owner)
                || region instanceof IWorldRegion
                || this.regions.containsValue(region)) return false;
        region.setOwner(owner);
        this.regions.put(owner, region);
        this.regionCache.markDirty(region, RegionCache.DirtyType.ADDED);
        FGStorageManagerNew.getInstance().addObject(region);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
        return true;
    }

    public boolean addRegion(IRegion region, @Nullable World world) {
        IOwner owner = region.getOwner();
        if (owner == null) owner = SERVER_OWNER;
        return addRegion(region, owner, world);
    }

    public boolean addRegion(IRegion region, IOwner owner, @Nullable World world) {
        if (region instanceof IWorldRegion) {
            return world != null && addWorldRegion((IWorldRegion) region, owner, world);
        } else return addRegion(region, owner);
    }

    public boolean addWorldRegion(IWorldRegion region, World world) {
        IOwner owner = region.getOwner();
        if (owner == null) owner = SERVER_OWNER;
        return addWorldRegion(region, owner, world);
    }

    public boolean addWorldRegion(IWorldRegion region, @Nonnull IOwner owner, World world) {
        checkNotNull(owner);
        if (region == null
                || world == null
                || region.getWorld() != null
                || !isNameValid(region.getName())
                || !isWorldRegionNameAvailable(region.getName(), owner, world))
            return false;
        region.setWorld(world);
        region.setOwner(owner);
        this.worldRegions.get(world).put(owner, region);
        this.regionCache.markDirty(region, RegionCache.DirtyType.ADDED);
        FGStorageManagerNew.getInstance().addObject(region);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
        return true;
    }

    public void clearRegionCache() {
        this.regionCache.clearCaches();
    }

    @Nonnull
    public Set<IRegion> getAllRegions() {
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.forEach((world, worldMultimap) -> set.addAll(worldMultimap.values()));
        set.addAll(this.regions.values());
        return ImmutableSet.copyOf(set);
    }

    @Nonnull
    public Set<IRegion> getAllRegions(@Nonnull IOwner owner) {
        checkNotNull(owner);
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.forEach((world, worldMultimap) -> set.addAll(worldMultimap.get(owner)));
        set.addAll(this.regions.get(owner));
        return ImmutableSet.copyOf(set);
    }

    @Nonnull
    public Set<IRegion> getAllRegions(World world) {
        if (world == null) return getRegions();
        Set<IRegion> set = new HashSet<>();
        set.addAll(this.worldRegions.get(world).values());
        set.addAll(this.regions.values());
        return ImmutableSet.copyOf(set);
    }

    @Nonnull
    public Set<IRegion> getAllRegions(String name, @Nonnull IOwner owner) {
        checkNotNull(owner);
        Set<IRegion> set = new HashSet<>();
        for (IRegion region : this.regions.get(owner)) {
            if (region.getName().equalsIgnoreCase(name)) set.add(region);
        }

        for (Multimap<IOwner, IWorldRegion> map : this.worldRegions.values()) {
            for (IWorldRegion region : map.get(owner)) {
                if (region.getName().equalsIgnoreCase(name)) set.add(region);
            }
        }
        return ImmutableSet.copyOf(set);
    }

    @Nonnull
    public Set<IRegion> getAllRegions(World world, @Nonnull IOwner owner) {
        checkNotNull(owner);
        if (world == null) return getRegions();
        Set<IRegion> set = new HashSet<>();
        set.addAll(this.worldRegions.get(world).get(owner));
        set.addAll(this.regions.get(owner));
        return ImmutableSet.copyOf(set);
    }

    @Nonnull
    public Set<IRegion> getAllRegionsWithUniqueNames(@Nonnull IOwner owner) {
        return getAllRegionsWithUniqueNames(owner, null);
    }

    @Nonnull
    public Set<IRegion> getAllRegionsWithUniqueNames(@Nonnull IOwner owner, @Nullable World world) {
        checkNotNull(owner);
        Set<IRegion> returnSet = new HashSet<>();
        returnSet.addAll(this.regions.get(owner));
        Map<String, Boolean> duplicates = new HashMap<>();
        Map<String, IRegion> regions = new HashMap<>();
        this.worldRegions.forEach((wld, map) -> map.get(owner).forEach(region -> {
            String name = region.getName();
            Boolean duplicate = duplicates.get(name);
            if (wld == world) {
                duplicates.put(name, true);
                regions.put(name, region);
            } else if (duplicate == null) {
                duplicates.put(name, false);
                regions.put(name, region);
            } else if (!duplicate) {
                duplicates.put(name, true);
                regions.remove(name);
            }
        }));
        returnSet.addAll(regions.values());
        return ImmutableSet.copyOf(returnSet);
    }

    @Nonnull
    public Set<IRegion> getAllServerRegions() {
        return getAllRegions(SERVER_OWNER);
    }

    @Nonnull
    public Set<IRegion> getAllServerRegions(World world) {
        return getAllRegions(world, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IController> getController(String name) {
        return getController(name, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IController> getController(String name, @Nonnull IOwner owner) {
        checkNotNull(owner);
        for (IHandler handler : handlers.get(owner)) {
            if ((handler instanceof IController) && handler.getName().equalsIgnoreCase(name)) {
                return Optional.of((IController) handler);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public Set<IController> getControllers() {
        return this.handlers.values().stream()
                .filter(handler -> handler instanceof IController)
                .map(handler -> ((IController) handler))
                .collect(GuavaCollectors.toImmutableSet());
    }

    @Nonnull
    public Set<IController> getControllers(@Nonnull IOwner owner) {
        checkNotNull(owner);
        return this.handlers.get(owner).stream()
                .filter(handler -> handler instanceof IController)
                .map(handler -> ((IController) handler))
                .collect(GuavaCollectors.toImmutableSet());
    }

    @Nonnull
    public GlobalHandler getGlobalHandler() {
        return globalHandler;
    }

    @Nonnull
    public Optional<IHandler> getHandler(String name) {
        return getHandler(name, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IHandler> getHandler(String name, @Nonnull IOwner owner) {
        checkNotNull(owner);
        for (IHandler handler : handlers.get(owner)) {
            if (handler.getName().equalsIgnoreCase(name)) {
                return Optional.of(handler);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public Set<IHandler> getHandlers() {
        return ImmutableSet.copyOf(this.handlers.values());
    }

    @Nonnull
    public Set<IHandler> getHandlers(@Nonnull IOwner owner) {
        checkNotNull(owner);
        return ImmutableSet.copyOf(this.handlers.get(owner));
    }

    @Nonnull
    public Set<IHandler> getHandlers(boolean includeControllers) {
        if (includeControllers) {
            return getHandlers();
        } else {
            return this.handlers.values().stream()
                    .filter(handler -> !(handler instanceof IController))
                    .collect(GuavaCollectors.toImmutableSet());
        }
    }

    @Nonnull
    public Set<IHandler> getHandlers(boolean includeControllers, IOwner owner) {
        if (includeControllers) {
            return getHandlers(owner);
        } else {
            return this.handlers.get(owner).stream()
                    .filter(handler -> !(handler instanceof IController))
                    .collect(GuavaCollectors.toImmutableSet());
        }
    }

    @Nonnull
    public Optional<IRegion> getRegion(String name) {
        return getRegion(name, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IRegion> getRegion(String name, IOwner owner) {
        for (IRegion region : this.regions.get(owner)) {
            if (region.getName().equalsIgnoreCase(name)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public Optional<IRegion> getRegionFromWorld(World world, String name) {
        return getRegionFromWorld(world, name, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IRegion> getRegionFromWorld(World world, String name, @Nonnull IOwner owner) {
        checkNotNull(owner);

        Optional<IWorldRegion> region = getWorldRegion(world, name, owner);
        return region.<Optional<IRegion>>map(Optional::of).orElseGet(() -> getRegion(name, owner));
    }

    @Nonnull
    public Set<IRegion> getRegions() {
        return ImmutableSet.copyOf(this.regions.values());
    }

    @Nonnull
    public Set<IRegion> getRegions(@Nonnull IOwner owner) {
        checkNotNull(owner);
        return ImmutableSet.copyOf(this.regions.get(owner));
    }

    @Nonnull
    public Set<IRegion> getRegionsAtMultiLocD(Iterable<Location<World>> locations) {
        return getRegionsAtMultiLocD(locations, false);
    }

    @Nonnull
    public Set<IRegion> getRegionsAtMultiLocD(Iterable<Location<World>> locations, boolean includeDisabled) {
        Set<IRegion> set = new HashSet<>();
        SetMultimap<Chunk, Vector3d> chunkPosMap = HashMultimap.create();
        for (Location<World> loc : locations) {
            Vector3i pos = loc.getBlockPosition();
            chunkPosMap.put(
                    new Chunk(
                            new Vector3i(
                                    pos.getX() >> 4,
                                    pos.getY() >> 4,
                                    pos.getZ() >> 4
                            ),
                            loc.getExtent()
                    ),
                    loc.getPosition()
            );
        }
        for (Map.Entry<Chunk, Collection<Vector3d>> entry : chunkPosMap.asMap().entrySet()) {
            Chunk chunk = entry.getKey();
            RegionCache.ChunkData data = this.regionCache.getData(chunk.world, chunk.chunk);
            Set<IRegion> candidates = new HashSet<>(data.getRegions(includeDisabled));
            candidates.removeAll(set);
            for (Vector3d pos : entry.getValue()) {
                if (candidates.isEmpty()) break;
                Iterator<IRegion> regionIterator = candidates.iterator();
                do {
                    IRegion region = regionIterator.next();
                    if (region.contains(pos, chunk.world)) {
                        set.add(region);
                        regionIterator.remove();
                    }
                } while (regionIterator.hasNext());
            }

        }
        return set;
    }

    @Nonnull
    public Set<IRegion> getRegionsAtMultiLocI(Iterable<Location<World>> locations) {
        return getRegionsAtMultiLocI(locations, false);
    }

    @Nonnull
    public Set<IRegion> getRegionsAtMultiLocI(Iterable<Location<World>> locations, boolean includeDisabled) {
        Set<IRegion> set = new HashSet<>();
        SetMultimap<Chunk, Vector3i> chunkPosMap = HashMultimap.create();
        for (Location<World> loc : locations) {
            Vector3i pos = loc.getBlockPosition();
            chunkPosMap.put(
                    new Chunk(
                            new Vector3i(
                                    pos.getX() >> 4,
                                    pos.getY() >> 4,
                                    pos.getZ() >> 4
                            ),
                            loc.getExtent()
                    ),
                    loc.getBlockPosition()
            );
        }
        for (Map.Entry<Chunk, Collection<Vector3i>> entry : chunkPosMap.asMap().entrySet()) {
            Chunk chunk = entry.getKey();
            RegionCache.ChunkData data = this.regionCache.getData(chunk.world, chunk.chunk);
            Set<IRegion> candidates = new HashSet<>(data.getRegions(includeDisabled));
            candidates.removeAll(set);
            for (Vector3i pos : entry.getValue()) {
                if (candidates.isEmpty()) break;
                Iterator<IRegion> regionIterator = candidates.iterator();
                do {
                    IRegion region = regionIterator.next();
                    if (region.contains(pos, chunk.world)) {
                        set.add(region);
                        regionIterator.remove();
                    }
                } while (regionIterator.hasNext());
            }

        }
        return set;
    }

    @Nonnull
    public Set<IRegion> getRegionsAtPos(World world, Vector3i position) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    @Nonnull
    public Set<IRegion> getRegionsAtPos(World world, Vector3i position, boolean includeDisabled) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position, includeDisabled).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    @Nonnull
    public Set<IRegion> getRegionsAtPos(World world, Vector3d position) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    @Nonnull
    public Set<IRegion> getRegionsAtPos(World world, Vector3d position, boolean includeDisabled) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position, includeDisabled).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunk(World world, Vector3i chunk) {
        return getRegionsInChunk(world, chunk, false);
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunk(World world, Vector3i chunk, boolean includeDisabled) {
        return this.regionCache.getData(world, chunk).getRegions(includeDisabled);
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3i pos) {
        return getRegionsInChunkAtPos(world, pos, false);
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3i pos, boolean includeDisabled) {
        return this.regionCache.getData(world,
                new Vector3i(
                        pos.getX() >> 4,
                        pos.getY() >> 4,
                        pos.getZ() >> 4)
        ).getRegions(includeDisabled);
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3d pos) {
        return getRegionsInChunkAtPos(world, pos, false);
    }

    @Nonnull
    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3d pos, boolean includeDisabled) {
        return this.regionCache.getData(world,
                new Vector3i(
                        GenericMath.floor(pos.getX()) >> 4,
                        GenericMath.floor(pos.getY()) >> 4,
                        GenericMath.floor(pos.getZ()) >> 4)
        ).getRegions(includeDisabled);
    }

    @Nonnull
    public Set<IController> getServerControllers() {
        return getControllers(SERVER_OWNER);
    }

    @Nonnull
    public Set<IHandler> getServerHandlers() {
        return getHandlers(SERVER_OWNER);
    }

    @Nonnull
    public Set<IHandler> getServerHandlers(boolean includeControllers) {
        return getHandlers(includeControllers, SERVER_OWNER);
    }

    @Nonnull
    public Set<IRegion> getServerRegions() {
        return getRegions(SERVER_OWNER);
    }

    @Nonnull
    public Set<IWorldRegion> getServerWorldRegions(World world) {
        return getWorldRegions(world, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IWorldRegion> getWorldRegion(World world, String name) {
        return getWorldRegion(world, name, SERVER_OWNER);
    }

    @Nonnull
    public Optional<IWorldRegion> getWorldRegion(World world, String name, @Nonnull IOwner owner) {
        checkNotNull(owner);
        for (IWorldRegion region : this.worldRegions.get(world).get(owner)) {
            if (region.getName().equalsIgnoreCase(name)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public Set<IWorldRegion> getWorldRegions(World world) {
        return ImmutableSet.copyOf(this.worldRegions.get(world).values());
    }

    @Nonnull
    public Set<IWorldRegion> getWorldRegions(World world, @Nonnull IOwner owner) {
        checkNotNull(owner);
        return ImmutableSet.copyOf(this.worldRegions.get(world).get(owner));
    }

    public void initWorld(World world) {
        GlobalWorldRegion gwr = new GlobalWorldRegion();
        gwr.setWorld(world);
        this.worldRegions.get(world).put(SERVER_OWNER, gwr);
        this.regionCache.markDirty(gwr, RegionCache.DirtyType.ADDED);
    }

    public boolean isHandlerNameAvailable(String name) {
        return isHandlerNameAvailable(name, SERVER_OWNER);
    }

    public boolean isHandlerNameAvailable(String name, @Nonnull IOwner owner) {
        return !getHandler(name, owner).isPresent();
    }

    public boolean isRegionNameAvailable(String name) {
        return isRegionNameAvailable(name, SERVER_OWNER);
    }

    public boolean isRegionNameAvailable(String name, @Nonnull IOwner owner) {
        if (getRegion(name, owner).isPresent()) return false;
        for (World world : worldRegions.keySet()) {
            if (getWorldRegion(world, name, owner).isPresent()) return false;
        }
        return true;
    }

    public boolean isRegistered(IHandler handler) {
        return handlers.containsValue(handler);
    }

    public boolean isWorldRegionNameAvailable(String name, World world) {
        return isWorldRegionNameAvailable(name, SERVER_OWNER, world);
    }

    public boolean isWorldRegionNameAvailable(String name, @Nonnull IOwner owner, World world) {
        return !(getWorldRegion(world, name, owner).isPresent() || getRegion(name, owner).isPresent());
    }

    @Nonnull
    public Tristate isWorldRegionNameAvailable(String name) {
        return isWorldRegionNameAvailable(name, SERVER_OWNER);
    }

    @Nonnull
    public Tristate isWorldRegionNameAvailable(String name, @Nonnull IOwner owner) {
        checkNotNull(owner);
        if (getRegion(name, owner).isPresent()) return Tristate.FALSE;
        Tristate available = null;
        for (World world : worldRegions.keySet()) {
            if (getWorldRegion(world, name, owner).isPresent()) {
                if (available == null) {
                    available = Tristate.FALSE;
                } else if (available == Tristate.TRUE) {
                    available = Tristate.UNDEFINED;
                }
            } else {
                if (available == null) {
                    available = Tristate.TRUE;
                } else if (available == Tristate.FALSE) {
                    available = Tristate.UNDEFINED;
                }
            }
        }
        if (available == null) available = Tristate.TRUE;
        return available;
    }

    public boolean link(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || linkable.getLinks().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
        return !(handler instanceof IGlobal) && linkable.addLink(handler);
    }

    public void markDirty(IRegion region, RegionCache.DirtyType type) {
        regionCache.markDirty(region, type);
    }

    public boolean removeObject(IGuardObject object) {
        if (object instanceof IRegion) return removeRegion(((IRegion) object));
        else return object instanceof IHandler && removeHandler(((IHandler) object));
    }

    public boolean removeHandler(IHandler handler) {
        if (handler == null || handler instanceof GlobalHandler) return false;
        this.worldRegions.forEach((world, set) -> set.values().stream()
                .filter(region -> region.getLinks().contains(handler))
                .forEach(region -> region.removeLink(handler)));
        if (!this.handlers.values().contains(handler)) {
            return false;
        }
        handlers.values().remove(handler);
        FGStorageManagerNew.getInstance().removeObject(handler);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), handler));
        return true;
    }

    public boolean removeRegion(IRegion region) {
        if (region instanceof IWorldRegion) {
            return removeWorldRegion((IWorldRegion) region);
        } else {
            if (region == null) return false;
            if (!this.regions.values().contains(region)) return false;
            this.regions.values().remove(region);
            FGStorageManagerNew.getInstance().removeObject(region);
            Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
            this.regionCache.markDirty(region, RegionCache.DirtyType.REMOVED);
            return true;
        }
    }

    public boolean removeWorldRegion(IWorldRegion region) {
        if (region == null || region instanceof GlobalWorldRegion) return false;
        boolean removed = false;
        if (region.getWorld() != null) {
            Collection<IWorldRegion> regions = this.worldRegions.get(region.getWorld()).values();
            if (!regions.contains(region)) {
                return false;
            }
            regions.remove(region);
            removed = true;
        } else {
            for (Multimap<IOwner, IWorldRegion> multimap : this.worldRegions.values()) {
                if (multimap.values().contains(region)) {
                    multimap.values().remove(region);
                    removed = true;
                }
            }
        }
        if (removed) {
            FGStorageManagerNew.getInstance().removeObject(region);
            Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
            this.regionCache.markDirty(region, RegionCache.DirtyType.REMOVED);
        }
        return removed;
    }

    public boolean move(IGuardObject object, @Nullable String newName, @Nullable IOwner newOwner, @Nullable World newWorld) {
        boolean changed = false, nameChanged = false, ownerChanged = false, worldChanged = false;
        String tryName = object.getName();
        if (newName != null && !newName.isEmpty() && isNameValid(newName)) {
            changed = true;
            nameChanged = true;
            tryName = newName;
        }
        IOwner tryOwner = object.getOwner();
        if (newOwner != null) {
            changed = true;
            ownerChanged = true;
            tryOwner = newOwner;
        }
        World tryWorld = null;
        if (object instanceof IWorldRegion && newWorld != null) {
            changed = true;
            worldChanged = true;
            tryWorld = ((IWorldRegion) object).getWorld();
        }
        if (!changed) return false;

        if (object instanceof IRegion) {
            if (object instanceof IWorldRegion) {
                if (!this.worldRegions.containsKey(tryWorld)) return false;
                if (!isWorldRegionNameAvailable(tryName, tryOwner, tryWorld)) return false;
            } else {
                if (this.getRegion(tryName, tryOwner).isPresent()) return false;
            }
        } else if (object instanceof IHandler) {
            if (this.getHandler(tryName, tryOwner).isPresent()) return false;
        }

        FGStorageManagerNew.getInstance().removeObject(object);
        if (nameChanged)
            object.setName(newName);
        if (ownerChanged || worldChanged) {
            if (object instanceof IHandler) {
                this.handlers.remove(object.getOwner(), object);
            } else if (object instanceof IRegion) {
                if (object instanceof IWorldRegion) {
                    this.worldRegions.get(((IWorldRegion) object).getWorld()).remove(object.getOwner(), object);
                } else {
                    this.regions.remove(object.getOwner(), object);
                }
            }
            if (ownerChanged)
                object.setOwner(newOwner);
            if (worldChanged) {
                ((IWorldRegion) object).setWorld(newWorld);
            }
            if (object instanceof IHandler) {
                this.handlers.put(object.getOwner(), (IHandler) object);
            } else if (object instanceof IRegion) {
                if (object instanceof IWorldRegion) {
                    this.worldRegions.get(((IWorldRegion) object).getWorld()).put(object.getOwner(), (IWorldRegion) object);
                } else {
                    this.regions.put(object.getOwner(), (IRegion) object);
                }
            }
            if (worldChanged) {
                IWorldRegion region = ((IWorldRegion) object);
                this.regionCache.markDirty(region, RegionCache.DirtyType.ADDED);
                Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
            }
        }
        FGStorageManagerNew.getInstance().addObject(object);
        return true;
    }

    public boolean unlink(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || !linkable.getLinks().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
        return !(handler instanceof IGlobal) && linkable.removeLink(handler);
    }

    public void unloadServer() {
        this.regions.clear();
        this.handlers.clear();
        this.regionCache.clearCaches();
    }

    public void unloadWorld(World world) {
        this.worldRegions.remove(world);
    }

    private static class Chunk {
        Vector3i chunk;
        World world;

        public Chunk(Vector3i chunk, World world) {
            this.chunk = chunk;
            this.world = world;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Chunk chunk1 = (Chunk) o;

            return chunk.equals(chunk1.chunk) && world.equals(chunk1.world);
        }

        @Override
        public int hashCode() {
            int result = chunk.hashCode();
            result = 31 * result + world.hashCode();
            return result;
        }
    }
}
