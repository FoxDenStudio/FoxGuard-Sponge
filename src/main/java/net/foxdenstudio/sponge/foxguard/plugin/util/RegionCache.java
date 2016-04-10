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
