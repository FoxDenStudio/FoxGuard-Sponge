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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FGManager {

    public static final String[] TYPES = {"region", "handler"};

    private static FGManager instance;
    private final Map<World, List<IRegion>> regions;
    private final List<IHandler> handlers;
    private final GlobalHandler globalHandler;

    private final Map<World, Map<Vector3i, List<IRegion>>> regionCache;

    private FGManager() {
        instance = this;
        regions = new CallbackHashMap<>((key, map) -> new ArrayList<>());
        handlers = new ArrayList<>();
        globalHandler = new GlobalHandler();
        this.addHandler(globalHandler);

        regionCache = new CallbackHashMap<>((world, map1) -> {
            if (world instanceof World) {
                Map<Vector3i, List<IRegion>> worldCache = new CallbackHashMap<>((chunk, map2) -> {
                    if (chunk instanceof Vector3i) {
                        List<IRegion> cachedRegions = this.calculateRegionsForChunk((Vector3i) chunk, (World) world);
                        map2.put((Vector3i) chunk, cachedRegions);
                        return cachedRegions;
                    } else return null;
                });
                map1.put((World) world, worldCache);
                return worldCache;
            } else return null;
        });
    }

    public static synchronized void init() {
        if (instance == null) instance = new FGManager();
    }

    public static FGManager getInstance() {
        return instance;
    }

    public boolean isRegistered(IHandler handler) {
        return handlers.contains(handler);
    }

    public boolean addRegion(World world, IRegion region) {
        if (region == null) return false;
        if (region.getWorld() != null || getRegion(world, region.getName()) != null) return false;
        region.setWorld(world);
        this.regions.get(world).add(region);
        this.regionCache.get(world).clear();
        FGStorageManager.getInstance().unmarkForDeletion(region);
        FGStorageManager.getInstance().updateRegion(region);
        return true;
    }

    public IRegion getRegion(World world, String name) {
        for (IRegion region : this.regions.get(world)) {
            if (region.getName().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    private IRegion getRegion(Server server, String worldName, String regionName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() ? this.getRegion(world.get(), regionName) : null;
    }

    public List<IRegion> getRegionsList() {
        List<IRegion> list = new ArrayList<>();
        this.regions.forEach((world, tlist) -> tlist.forEach(list::add));
        return ImmutableList.copyOf(list);
    }

    public List<IRegion> getRegionsList(World world) {
        return ImmutableList.copyOf(this.regions.get(world));
    }

    public List<IRegion> getRegionsList(World world, Vector3i chunk) {
        return ImmutableList.copyOf(this.regionCache.get(world).get(chunk));
    }

    public List<IHandler> getHandlerList() {
        return ImmutableList.copyOf(this.handlers);
    }

    public boolean addHandler(IHandler handler) {
        if (handler == null) return false;
        if (gethandler(handler.getName()) != null) return false;
        handlers.add(handler);
        FGStorageManager.getInstance().unmarkForDeletion(handler);
        FGStorageManager.getInstance().updateHandler(handler);
        return true;
    }

    public IHandler gethandler(String name) {
        for (IHandler handler : handlers) {
            if (handler.getName().equalsIgnoreCase(name)) {
                return handler;
            }
        }
        return null;
    }

    public boolean removeHandler(String name) {
        return this.removeHandler(gethandler(name));
    }

    private boolean removeHandler(IHandler handler) {
        if (handler == null || handler instanceof GlobalHandler) return false;
        this.regions.forEach((world, list) -> {
            list.stream()
                    .filter(region -> region.getHandlers().contains(handler))
                    .forEach(region -> region.removeHandler(handler));
        });
        if (!this.handlers.contains(handler)) {
            return false;
        }
        FGStorageManager.getInstance().markForDeletion(handler);
        handlers.remove(handler);
        return true;
    }

    public boolean removeRegion(World world, String name) {
        return this.removeRegion(world, getRegion(world, name));
    }

    public void removeRegion(IRegion region) {
        if (region.getWorld() != null) removeRegion(region.getWorld(), region);
        else this.regions.keySet().forEach((world) -> this.removeRegion(world, region));
    }

    private boolean removeRegion(World world, IRegion region) {
        if (region == null || region instanceof GlobalRegion) return false;
        if (!this.regions.get(world).contains(region)) {
            return false;
        }
        FGStorageManager.getInstance().markForDeletion(region);
        this.regions.get(world).remove(region);
        this.regionCache.get(world).clear();
        return true;
    }

    public boolean link(Server server, String worldName, String regionName, String handlerName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.link(world.get(), regionName, handlerName);
    }

    public boolean link(World world, String regionName, String handlerName) {
        IRegion region = getRegion(world, regionName);
        IHandler handler = gethandler(handlerName);
        return this.link(region, handler);
    }

    public boolean link(IRegion region, IHandler handler) {
        if (region == null || handler == null || region.getHandlers().contains(handler)) return false;
        return !(handler instanceof GlobalHandler && !(region instanceof GlobalRegion)) && region.addHandler(handler);
    }

    public boolean unlink(Server server, String worldName, String regionName, String handlerName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.unlink(world.get(), regionName, handlerName);
    }

    private boolean unlink(World world, String regionName, String handlerName) {
        IRegion region = getRegion(world, regionName);
        IHandler handler = gethandler(handlerName);
        return this.unlink(region, handler);
    }

    public boolean unlink(IRegion region, IHandler handler) {
        if (region == null || handler == null || !region.getHandlers().contains(handler)) return false;
        return !(handler instanceof GlobalHandler && region instanceof GlobalRegion) && region.removeHandler(handler);
    }

    public boolean renameRegion(World world, String oldName, String newName) {
        return this.rename(this.getRegion(world, oldName), newName);
    }

    public boolean renameRegion(Server server, String worldName, String oldName, String newName) {
        return this.rename(this.getRegion(server, worldName, oldName), newName);
    }

    private boolean rename(IFGObject object, String newName) {
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            if (this.getRegion(region.getWorld(), newName) != null) return false;
        } else if (object instanceof IHandler) {
            if (this.gethandler(newName) != null) return false;
        }
        FGStorageManager.getInstance().markForDeletion(object);
        object.setName(newName);
        FGStorageManager.getInstance().unmarkForDeletion(object);
        FGStorageManager.getInstance().update(object);
        return true;
    }

    public boolean renameHandler(String oldName, String newName) {
        return this.rename(this.gethandler(oldName), newName);
    }

    private void createLists(World world) {
        regions.put(world, new ArrayList<>());
    }

    public void populateWorld(World world) {
        this.createLists(world);
        GlobalRegion gr = new GlobalRegion();
        gr.addHandler(this.globalHandler);
        addRegion(world, gr);
    }

    public void unloadWorld(World world){
        this.regions.remove(world);
    }

    public GlobalHandler getGlobalHandler() {
        return globalHandler;
    }

    private List<IRegion> calculateRegionsForChunk(Vector3i chunk, World world) {
        List<IRegion> cache = new ArrayList<>();
        this.getRegionsList(world).stream()
                .filter(region -> region.isInChunk(chunk))
                .forEach(cache::add);
        return cache;
    }

    public void clearCache(World world) {
        this.regionCache.get(world).clear();
    }
}
