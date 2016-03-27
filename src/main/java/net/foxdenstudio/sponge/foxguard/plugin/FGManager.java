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
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateEvent;
import net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateObjectEvent;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.region.GlobalRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FGManager {

    public static final String[] TYPES = {"region", "handler", "controller"};

    private static FGManager instance;
    private final Map<World, List<IRegion>> regions;
    private final List<IHandler> handlers;
    private final GlobalHandler globalHandler;

    private final Map<World, Map<Vector3i, List<IRegion>>> regionCache;

    private FGManager() {
        instance = this;
        regions = new CacheMap<>((key, map) -> new ArrayList<>());
        handlers = new ArrayList<>();
        globalHandler = new GlobalHandler();
        this.addHandler(globalHandler);

        regionCache = new CacheMap<>((world, map1) -> {
            if (world instanceof World) {
                Map<Vector3i, List<IRegion>> worldCache = new CacheMap<>((chunk, map2) -> {
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

    public boolean addRegion(World world, IRegion region, boolean update) {
        if (region == null) return false;
        if (region.getWorld() != null || getRegion(world, region.getName()) != null) return false;
        region.setWorld(world);
        this.regions.get(world).add(region);
        this.regionCache.get(world).clear();
        Sponge.getGame().getEventManager().post(new FGUpdateObjectEvent() {
            @Override
            public IFGObject getTarget() {
                return region;
            }

            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
        FGStorageManager.getInstance().unmarkForDeletion(region);
        if (update) FGStorageManager.getInstance().updateRegion(region);
        return true;
    }

    public boolean addRegion(World world, IRegion region) {
        return addRegion(world, region, true);
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

    public List<IRegion> getRegionList() {
        List<IRegion> list = new ArrayList<>();
        this.regions.forEach((world, tlist) -> tlist.forEach(list::add));
        return ImmutableList.copyOf(list);
    }

    public List<IRegion> getRegionList(World world) {
        return ImmutableList.copyOf(this.regions.get(world));
    }

    public List<IRegion> getRegionList(World world, Vector3i chunk) {
        return ImmutableList.copyOf(this.regionCache.get(world).get(chunk));
    }

    public List<IRegion> getRegionListAtPos(World world, Vector3d pos) {
        return getRegionListAtPos(world, pos, false);
    }

    public List<IRegion> getRegionListAtPos(World world, Vector3d pos, boolean includeDisabled) {
        Vector3i chunk = new Vector3i(
                GenericMath.floor(pos.getX() / 16.0),
                GenericMath.floor(pos.getY() / 16.0),
                GenericMath.floor(pos.getZ() / 16.0));
        if (includeDisabled)
            return this.regionCache.get(world).get(chunk).stream()
                    .filter(region -> region.contains(pos))
                    .collect(GuavaCollectors.toImmutableList());
        else
            return this.regionCache.get(world).get(chunk).stream()
                    .filter(IFGObject::isEnabled)
                    .filter(region -> region.contains(pos))
                    .collect(GuavaCollectors.toImmutableList());
    }

    public List<IRegion> getRegionListAtPos(World world, Vector3i pos) {
        return getRegionListAtPos(world, pos, false);
    }

    public List<IRegion> getRegionListAtPos(World world, Vector3i pos, boolean includeDisabled) {
        Vector3i chunk = new Vector3i(
                GenericMath.floor(pos.getX() / 16.0),
                GenericMath.floor(pos.getY() / 16.0),
                GenericMath.floor(pos.getZ() / 16.0));
        if (includeDisabled)
            return this.regionCache.get(world).get(chunk).stream()
                    .filter(region -> region.contains(pos))
                    .collect(GuavaCollectors.toImmutableList());
        else
            return this.regionCache.get(world).get(chunk).stream()
                    .filter(IFGObject::isEnabled)
                    .filter(region -> region.contains(pos))
                    .collect(GuavaCollectors.toImmutableList());
    }

    public List<IHandler> getHandlerList() {
        return ImmutableList.copyOf(this.handlers);
    }

    public List<IHandler> getHandlerList(boolean includeControllers) {
        if (includeControllers) {
            return ImmutableList.copyOf(this.handlers);
        } else {
            return this.handlers.stream()
                    .filter(handler -> !(handler instanceof IController))
                    .collect(GuavaCollectors.toImmutableList());
        }
    }

    public List<IController> getControllerList() {
        return this.handlers.stream()
                .filter(handler -> handler instanceof IController)
                .map(handler -> ((IController) handler))
                .collect(GuavaCollectors.toImmutableList());
    }

    public boolean addHandler(IHandler handler) {
        if (handler == null) return false;
        if (gethandler(handler.getName()) != null) return false;
        handlers.add(handler);
        Sponge.getGame().getEventManager().post(new FGUpdateObjectEvent() {
            @Override
            public IFGObject getTarget() {
                return handler;
            }

            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
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

    public IController getController(String name) {
        for (IHandler handler : handlers) {
            if ((handler instanceof IController) && handler.getName().equalsIgnoreCase(name)) {
                return (IController) handler;
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
        Sponge.getGame().getEventManager().post(new FGUpdateObjectEvent() {
            @Override
            public IFGObject getTarget() {
                return handler;
            }

            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
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
        Sponge.getGame().getEventManager().post(new FGUpdateObjectEvent() {
            @Override
            public IFGObject getTarget() {
                return region;
            }

            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
        FGStorageManager.getInstance().markForDeletion(region);
        this.regions.get(world).remove(region);
        this.regionCache.get(world).clear();
        return true;
    }

    public boolean linkRegion(Server server, String worldName, String regionName, String handlerName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.linkRegion(world.get(), regionName, handlerName);
    }

    public boolean linkRegion(World world, String regionName, String handlerName) {
        IRegion region = getRegion(world, regionName);
        IHandler handler = gethandler(handlerName);
        return this.link(region, handler);
    }

    public boolean link(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || linkable.getHandlers().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(new FGUpdateEvent() {
            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
        return !(handler instanceof GlobalHandler && !(linkable instanceof GlobalRegion)) && linkable.addHandler(handler);
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

    public boolean unlink(ILinkable linkable, IHandler handler) {
        if (linkable == null || handler == null || !linkable.getHandlers().contains(handler)) return false;
        Sponge.getGame().getEventManager().post(new FGUpdateEvent() {
            @Override
            public Cause getCause() {
                return FoxGuardMain.getCause();
            }
        });
        return !(handler instanceof GlobalHandler && linkable instanceof GlobalRegion) && linkable.removeHandler(handler);
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

    public void createLists(World world) {
        regions.put(world, new ArrayList<>());
    }

    public void initWorld(World world) {
        GlobalRegion gr = new GlobalRegion();
        gr.addHandler(this.globalHandler);
        addRegion(world, gr, false);
    }

    public void unloadWorld(World world) {
        this.regions.remove(world);
    }

    public GlobalHandler getGlobalHandler() {
        return globalHandler;
    }

    private List<IRegion> calculateRegionsForChunk(Vector3i chunk, World world) {
        List<IRegion> cache = new ArrayList<>();
        this.getRegionList(world).stream()
                .filter(IFGObject::isEnabled)
                .filter(region -> region.isInChunk(chunk))
                .forEach(cache::add);
        return cache;
    }

    public void clearCache(World world) {
        this.regionCache.get(world).clear();
    }
}
