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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxcore.plugin.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Fox on 3/30/2016.
 */

//TODO Complete caching behavior in FGManager
public class RegionCache {

    private final Map<World, Set<IWorldRegion>> worldRegions;
    private final Set<IRegion> regions;

    private final Map<World, Map<Vector3i, ChunkData>> chunks;

    public RegionCache(Set<IRegion> regions, Map<World, Set<IWorldRegion>> worldRegions) {
        this.worldRegions = worldRegions;
        this.regions = regions;
        chunks = new CacheMap<>((world, worldDataMap) -> {
            if (world instanceof World) {
                Map<Vector3i, ChunkData> worldData = new CacheMap<>((chunk, chunkDataMap) -> {
                    if (chunk instanceof Vector3i) {
                        ChunkData data = new ChunkData((World) world, (Vector3i) chunk);
                        chunkDataMap.put((Vector3i) chunk, data);
                        return data;
                    } else return null;
                });
                worldDataMap.put((World) world, worldData);
                return worldData;
            } else return null;
        });
    }

    public void markDirty(IRegion region, DirtyType type) {
        if (region instanceof IWorldRegion) {
            for (ChunkData data : chunks.get(((IWorldRegion) region).getWorld()).values()) {
                data.markDirty(region, type);
            }
        } else {
            for (Map<Vector3i, ChunkData> worldData : chunks.values()) {
                for (ChunkData data : worldData.values()) {
                    data.markDirty(region, type);
                }
            }
        }
    }

    public void clearCaches() {
        this.chunks.values().forEach(Map::clear);
    }

    public ChunkData getData(World world, Vector3i chunk) {
        return this.chunks.get(world).get(chunk);
    }

    public class ChunkData {

        private final World world;
        private final Vector3i chunk;

        private final Set<IRegion> contains;
        private final Set<IRegion> disabled;
        private final Map<IRegion, DirtyType> dirty;
        private boolean isDirty = false;


        public ChunkData(World world, Vector3i chunk) {
            this.world = world;
            this.chunk = chunk;
            this.dirty = new HashMap<>();
            this.contains = new HashSet<>();
            worldRegions.get(world).stream()
                    .filter(IFGObject::isEnabled)
                    .filter(r -> r.isInChunk(chunk))
                    .forEach(contains::add);
            regions.stream()
                    .filter(IFGObject::isEnabled)
                    .filter(r -> r.isInChunk(chunk, world))
                    .forEach(contains::add);
            this.disabled = new HashSet<>();
            worldRegions.get(world).stream()
                    .filter(r -> !r.isEnabled())
                    .filter(r -> r.isInChunk(chunk))
                    .forEach(disabled::add);
            regions.stream()
                    .filter(IFGObject::isEnabled)
                    .filter(r -> r.isInChunk(chunk, world))
                    .forEach(disabled::add);
        }

        public Set<IRegion> getRegions(boolean includeDisabled) {
            if (this.isDirty) {
                for (Map.Entry<IRegion, DirtyType> entry : dirty.entrySet()) {
                    IRegion r = entry.getKey();
                    switch (entry.getValue()) {
                        case ADDED:
                            if (r.isInChunk(chunk, world)) {
                                if (r.isEnabled()) {
                                    contains.add(r);
                                } else {
                                    disabled.add(r);
                                }
                            }
                            break;
                        case MODIFIED:
                            if (r.isInChunk(chunk, world)) {
                                if (r.isEnabled()) {
                                    contains.add(r);
                                    disabled.remove(r);
                                } else {
                                    contains.remove(r);
                                    disabled.add(r);
                                }
                            } else {
                                contains.remove(r);
                                disabled.remove(r);
                            }
                            break;
                        case REMOVED:
                            contains.remove(r);
                            disabled.remove(r);
                            break;
                    }
                }
                this.dirty.clear();
                this.isDirty = false;
            }
            if (includeDisabled) return ImmutableSet.<IRegion>builder().addAll(contains).addAll(disabled).build();
            else return ImmutableSet.copyOf(contains);
        }

        public void markDirty(IRegion region, DirtyType type) {
            dirty.put(region, type);
            isDirty = true;
        }

    }

    public enum DirtyType {
        ADDED, MODIFIED, REMOVED
    }
}
