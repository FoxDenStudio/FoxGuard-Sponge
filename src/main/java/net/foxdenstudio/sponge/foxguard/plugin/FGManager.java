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
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.GlobalWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.RegionCache;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.stream.Collectors;

public final class FGManager {

    public static final UUID SERVER_UUID = new UUID(0, 0);
    public static final String[] ILLEGAL_NAMES = {"all", "state", "full", "everything"};

    private static FGManager instance;
    private final Map<World, Multimap<UUID, IWorldRegion>> worldRegions;
    private final Multimap<UUID, IRegion> regions;
    private final Multimap<UUID, IHandler> handlers;
    private final GlobalRegion globalRegion;
    private final GlobalHandler globalHandler;

    private final RegionCache regionCache;

    private FGManager() {
        instance = this;
        worldRegions = new CacheMap<>((key, map) -> {
            if (key instanceof World) {
                Multimap<UUID, IWorldRegion> uuidMap = HashMultimap.create();
                map.put((World) key, uuidMap);
                return uuidMap;
            } else return null;
        });
        regions = HashMultimap.create();
        handlers = HashMultimap.create();
        globalRegion = new GlobalRegion();
        globalHandler = new GlobalHandler();
        regions.put(SERVER_UUID, globalRegion);
        handlers.put(SERVER_UUID, globalHandler);
        globalRegion.addLink(globalHandler);

        this.regionCache = new RegionCache(regions, worldRegions);
    }

    public static void init() {
        if (instance == null) instance = new FGManager();
        if (instance.regions.isEmpty()) instance.regions.put(SERVER_UUID, instance.globalRegion);
        if (instance.handlers.isEmpty()) instance.handlers.put(SERVER_UUID, instance.globalHandler);
    }

    public static FGManager getInstance() {
        return instance;
    }

    public static boolean isNameValid(String name) {
        return !name.matches("^.*[ :.=;\"\'\\\\/{}()\\[\\]<>#@|?*].*$") &&
                !Aliases.isIn(FGStorageManager.FS_ILLEGAL_NAMES, name) &&
                !Aliases.isIn(ILLEGAL_NAMES, name);
    }

    public boolean isRegistered(IHandler handler) {
        return handlers.containsValue(handler);
    }

    public boolean isRegionNameAvailable(String name) {
        if (getRegion(name).isPresent()) return false;
        for (World world : worldRegions.keySet()) {
            if (getWorldRegion(world, name).isPresent()) return false;
        }
        return true;
    }

    public boolean isWorldRegionNameAvailable(String name, World world) {
        return !(getWorldRegion(world, name).isPresent() || getRegion(name).isPresent());
    }

    public Tristate isWorldRegionNameAvailable(String name) {
        Tristate available = null;
        for (World world : worldRegions.keySet()) {
            if (!getWorldRegion(world, name).isPresent()) {
                if (available == null) {
                    available = Tristate.TRUE;
                } else if (available == Tristate.FALSE) {
                    available = Tristate.UNDEFINED;
                }
            } else {
                if (available == null) {
                    available = Tristate.FALSE;
                } else if (available == Tristate.TRUE) {
                    available = Tristate.UNDEFINED;
                }
            }
        }
        return available;
    }

    public boolean addWorldRegion(World world, IWorldRegion region) {
        return addWorldRegion(world, region, SERVER_UUID);
    }

    public boolean addWorldRegion(World world, IWorldRegion region, UUID uuid) {
        if (region == null || region.getWorld() != null ||
                !isWorldRegionNameAvailable(region.getName(), world) || !isNameValid(region.getName()))
            return false;
        region.setWorld(world);
        this.worldRegions.get(world).put(uuid, region);
        this.regionCache.markDirty(region, RegionCache.DirtyType.ADDED);
        FGStorageManager.getInstance().addObject(region);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
        return true;
    }

    public boolean addRegion(IRegion region) {
        return addRegion(region, SERVER_UUID);
    }

    public boolean addRegion(IRegion region, UUID uuid) {
        if (region == null || !isRegionNameAvailable(region.getName()) || !isNameValid(region.getName())) return false;
        this.regions.put(uuid, region);
        this.regionCache.markDirty(region, RegionCache.DirtyType.ADDED);
        FGStorageManager.getInstance().addObject(region);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
        return true;
    }

    public boolean addRegion(IRegion region, World world) {
        return addRegion(region, world, SERVER_UUID);
    }

    public boolean addRegion(IRegion region, World world, UUID uuid) {
        if (region instanceof IWorldRegion) {
            return world != null && addWorldRegion(world, (IWorldRegion) region, uuid);
        } else return addRegion(region, uuid);
    }

    public Optional<IWorldRegion> getWorldRegion(World world, String name) {
        return getWorldRegion(world, name, SERVER_UUID);
    }

    public Optional<IWorldRegion> getWorldRegion(World world, String name, UUID uuid) {
        for (IWorldRegion region : this.worldRegions.get(world).get(uuid)) {
            if (region.getName().equalsIgnoreCase(name)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    public Optional<IRegion> getRegion(String name) {
        return getRegion(name, SERVER_UUID);
    }

    public Optional<IRegion> getRegion(String name, UUID uuid) {
        for (IRegion region : this.regions.get(uuid)) {
            if (region.getName().equalsIgnoreCase(name)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    public Optional<IRegion> getRegionFromWorld(World world, String name) {
        return getRegionFromWorld(world, name, SERVER_UUID);
    }

    public Optional<IRegion> getRegionFromWorld(World world, String name, UUID uuid) {
        Optional<IWorldRegion> region = getWorldRegion(world, name, uuid);
        if (!region.isPresent()) {
            return getRegion(name, uuid);
        } else return Optional.of(region.get());
    }

    public Set<IRegion> getRegions() {
        return ImmutableSet.copyOf(this.regions.values());
    }

    public Set<IRegion> getServerRegions() {
        return getRegions(SERVER_UUID);
    }

    public Set<IRegion> getRegions(UUID uuid) {
        return ImmutableSet.copyOf(this.regions.get(uuid));
    }

    public Set<IWorldRegion> getWorldRegions(World world) {
        return ImmutableSet.copyOf(this.worldRegions.get(world).values());
    }

    public Set<IWorldRegion> getServerWorldRegions(World world) {
        return getWorldRegions(world, SERVER_UUID);
    }

    public Set<IWorldRegion> getWorldRegions(World world, UUID uuid) {
        return ImmutableSet.copyOf(this.worldRegions.get(world).get(uuid));
    }

    public Set<IRegion> getAllRegions() {
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.forEach((world, worldMultimap) -> worldMultimap.values().forEach(set::add));
        this.regions.values().forEach(set::add);
        return ImmutableSet.copyOf(set);
    }

    public Set<IRegion> getAllServerRegions() {
        return getAllRegions(SERVER_UUID);
    }

    public Set<IRegion> getAllRegions(UUID uuid) {
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.forEach((world, worldMultimap) -> worldMultimap.get(uuid).forEach(set::add));
        this.regions.get(uuid).forEach(set::add);
        return ImmutableSet.copyOf(set);
    }

    public Set<IRegion> getAllServerRegions(World world) {
        return getAllRegions(world, SERVER_UUID);
    }

    public Set<IRegion> getAllRegions(World world) {
        if (world == null) return getRegions();
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.get(world).values().forEach(set::add);
        this.regions.values().forEach(set::add);
        return ImmutableSet.copyOf(set);
    }

    public Set<IRegion> getAllRegions(World world, UUID uuid) {
        if (world == null) return getRegions();
        Set<IRegion> set = new HashSet<>();
        this.worldRegions.get(world).get(uuid).forEach(set::add);
        this.regions.get(uuid).forEach(set::add);
        return ImmutableSet.copyOf(set);
    }

    public Set<IRegion> getRegionsInChunk(World world, Vector3i chunk) {
        return getRegionsInChunk(world, chunk, false);
    }

    public Set<IRegion> getRegionsInChunk(World world, Vector3i chunk, boolean includeDisabled) {
        return this.regionCache.getData(world, chunk).getRegions(includeDisabled);
    }

    public Set<IRegion> getRegionsAtPos(World world, Vector3i position) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    public Set<IRegion> getRegionsAtPos(World world, Vector3i position, boolean includeDisabled) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position, includeDisabled).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    public Set<IRegion> getRegionsAtPos(World world, Vector3d position) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    public Set<IRegion> getRegionsAtPos(World world, Vector3d position, boolean includeDisabled) {
        return FGManager.getInstance().getRegionsInChunkAtPos(world, position, includeDisabled).stream()
                .filter(region -> region.contains(position, world))
                .collect(Collectors.toSet());
    }

    public Set<IRegion> getRegionsAtMultiLocI(Iterable<Location<World>> locations) {
        return getRegionsAtMultiLocI(locations, false);
    }

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

    public Set<IRegion> getRegionsAtMultiLocD(Iterable<Location<World>> locations) {
        return getRegionsAtMultiLocD(locations, false);
    }

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

    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3i pos) {
        return getRegionsInChunkAtPos(world, pos, false);
    }

    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3i pos, boolean includeDisabled) {
        return this.regionCache.getData(world,
                new Vector3i(
                        pos.getX() >> 4,
                        pos.getY() >> 4,
                        pos.getZ() >> 4)
        ).getRegions(includeDisabled);
    }

    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3d pos) {
        return getRegionsInChunkAtPos(world, pos, false);
    }

    public Set<IRegion> getRegionsInChunkAtPos(World world, Vector3d pos, boolean includeDisabled) {
        return this.regionCache.getData(world,
                new Vector3i(
                        GenericMath.floor(pos.getX()) >> 4,
                        GenericMath.floor(pos.getY()) >> 4,
                        GenericMath.floor(pos.getZ()) >> 4)
        ).getRegions(includeDisabled);
    }

    public Set<IHandler> getHandlers() {
        return ImmutableSet.copyOf(this.handlers.values());
    }

    public Set<IHandler> getServerHandlers() {
        return getHandlers(SERVER_UUID);
    }

    public Set<IHandler> getHandlers(UUID uuid) {
        return ImmutableSet.copyOf(this.handlers.get(uuid));
    }

    public Set<IHandler> getHandlers(boolean includeControllers) {
        if (includeControllers) {
            return getHandlers();
        } else {
            return this.handlers.values().stream()
                    .filter(handler -> !(handler instanceof IController))
                    .collect(GuavaCollectors.toImmutableSet());
        }
    }

    public Set<IHandler> getServerHandlers(boolean includeControllers) {
        return getHandlers(includeControllers, SERVER_UUID);
    }

    public Set<IHandler> getHandlers(boolean includeControllers, UUID uuid) {
        if (includeControllers) {
            return getHandlers(uuid);
        } else {
            return this.handlers.get(uuid).stream()
                    .filter(handler -> !(handler instanceof IController))
                    .collect(GuavaCollectors.toImmutableSet());
        }
    }

    public Set<IController> getControllers() {
        return this.handlers.values().stream()
                .filter(handler -> handler instanceof IController)
                .map(handler -> ((IController) handler))
                .collect(GuavaCollectors.toImmutableSet());
    }

    public Set<IController> getServerControllers() {
        return getControllers(SERVER_UUID);
    }

    public Set<IController> getControllers(UUID uuid) {
        return this.handlers.get(uuid).stream()
                .filter(handler -> handler instanceof IController)
                .map(handler -> ((IController) handler))
                .collect(GuavaCollectors.toImmutableSet());
    }

    public boolean addHandler(IHandler handler) {
        return addHandler(handler, SERVER_UUID);
    }

    public boolean addHandler(IHandler handler, UUID uuid) {
        if (handler == null) return false;
        if (gethandler(handler.getName()).isPresent()) return false;
        handlers.put(uuid, handler);
        FGStorageManager.getInstance().addObject(handler);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), handler));
        return true;
    }

    public Optional<IHandler> gethandler(String name) {
        return gethandler(name, SERVER_UUID);
    }

    public Optional<IHandler> gethandler(String name, UUID uuid) {
        for (IHandler handler : handlers.get(uuid)) {
            if (handler.getName().equalsIgnoreCase(name)) {
                return Optional.of(handler);
            }
        }
        return Optional.empty();
    }

    public Optional<IController> getController(String name) {
        return getController(name, SERVER_UUID);
    }

    public Optional<IController> getController(String name, UUID uuid) {
        for (IHandler handler : handlers.get(uuid)) {
            if ((handler instanceof IController) && handler.getName().equalsIgnoreCase(name)) {
                return Optional.of((IController) handler);
            }
        }
        return Optional.empty();
    }

    public boolean removeHandler(IHandler handler) {
        if (handler == null || handler instanceof GlobalHandler) return false;
        this.worldRegions.forEach((world, set) -> set.values().stream()
                .filter(region -> region.getLinks().contains(handler))
                .forEach(region -> region.removeLink(handler)));
        if (!this.handlers.values().contains(handler)) {
            return false;
        }
        FGStorageManager.getInstance().removeObject(handler);
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), handler));
        handlers.values().remove(handler);
        return true;
    }

    public boolean removeRegion(IRegion region) {
        if (region instanceof IWorldRegion) {
            return removeWorldRegion((IWorldRegion) region);
        } else {
            if (region == null) return false;
            if (!this.regions.values().contains(region)) return false;
            this.regions.values().remove(region);
            FGStorageManager.getInstance().removeObject(region);
            Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
            this.regionCache.markDirty(region, RegionCache.DirtyType.REMOVED);
            return true;
        }
    }

    public boolean removeWorldRegion(IWorldRegion region) {
        if (region == null || region instanceof GlobalWorldRegion) return false;
        boolean removed = false;
        if (region.getWorld() != null) {
            if (!this.worldRegions.get(region.getWorld()).values().contains(region)) {
                return false;
            }
            this.worldRegions.get(region.getWorld()).values().remove(region);
            removed = true;
        } else {
            for (Multimap<UUID, IWorldRegion> multimap : this.worldRegions.values()) {
                if (multimap.values().contains(region)) {
                    multimap.values().remove(region);
                    removed = true;
                }
            }
        }
        if (removed) {
            FGStorageManager.getInstance().removeObject(region);
            Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateObjectEvent(FoxGuardMain.getCause(), region));
            this.regionCache.markDirty(region, RegionCache.DirtyType.REMOVED);
        }
        return removed;
    }

    public boolean link(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || linkable.getLinks().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
        return !(handler instanceof IGlobal) && linkable.addLink(handler);
    }

    public boolean unlink(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || !linkable.getLinks().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(FGEventFactory.createFGUpdateEvent(FoxGuardMain.getCause()));
        return !(handler instanceof IGlobal) && linkable.removeLink(handler);
    }

    public boolean rename(IFGObject object, String newName) {
        if (object instanceof IRegion) {
            if (object instanceof IWorldRegion) {
                IWorldRegion region = (IWorldRegion) object;
                if (!isWorldRegionNameAvailable(newName, region.getWorld())) return false;
            } else {
                if (this.getRegion(newName).isPresent()) return false;
            }
        } else if (object instanceof IHandler) {
            if (this.gethandler(newName).isPresent()) return false;
        }
        FGStorageManager.getInstance().removeObject(object);
        object.setName(newName);
        FGStorageManager.getInstance().addObject(object);
        return true;
    }

    public void initWorld(World world) {
        GlobalWorldRegion gwr = new GlobalWorldRegion();
        gwr.setWorld(world);
        this.worldRegions.get(world).put(SERVER_UUID, gwr);
        this.regionCache.markDirty(gwr, RegionCache.DirtyType.ADDED);
    }

    public void unloadWorld(World world) {
        this.worldRegions.remove(world);
    }

    public void unloadServer() {
        this.regions.clear();
        this.handlers.clear();
        this.regionCache.clearCaches();
    }

    public GlobalHandler getGlobalHandler() {
        return globalHandler;
    }

    public void markDirty(IRegion region, RegionCache.DirtyType type) {
        regionCache.markDirty(region, type);
    }

    public void clearRegionCache() {
        this.regionCache.clearCaches();
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
