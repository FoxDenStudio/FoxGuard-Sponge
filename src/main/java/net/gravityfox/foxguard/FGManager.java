/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard;

import com.google.common.collect.ImmutableList;
import net.gravityfox.foxguard.handlers.GlobalHandler;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.GlobalRegion;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.CallbackHashMap;
import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class FGManager {

    private static FGManager instance;
    private final Map<World, List<IRegion>> regions;
    private final List<IHandler> handlers;
    private final Map<World, ReadWriteLock> regionLocks;
    private final ReadWriteLock handlerLock;
    private final GlobalHandler globalHandler;

    private FGManager() {
        instance = this;
        regionLocks = new CallbackHashMap<>((key, map) -> {
            if (key instanceof World) {
                ReadWriteLock lock = FoxGuardMain.getNewLock();
                map.put((World) key, lock);
                return lock;
            } else return null;
        });
        handlerLock = FoxGuardMain.getNewLock();
        regions = new CallbackHashMap<>((key, map) -> new LinkedList<>());
        handlers = new LinkedList<>();
        globalHandler = new GlobalHandler();
        this.addHandler(globalHandler);
    }

    public static synchronized void init() {
        if (instance == null) instance = new FGManager();
    }

    public static FGManager getInstance() {
        return instance;
    }

    public boolean isRegistered(IHandler handler) {
        try {
            this.handlerLock.readLock().lock();
            return handlers.contains(handler);
        } finally {
            this.handlerLock.readLock().unlock();
        }
    }

    public boolean addRegion(World world, IRegion region) {
        if (region == null) return false;
        if (region.getWorld() != null || getRegion(world, region.getName()) != null) return false;
        region.setWorld(world);
        ReadWriteLock lock = this.regionLocks.get(world);
        try {
            lock.writeLock().lock();
            this.regions.get(world).add(region);
        } finally {
            lock.writeLock().unlock();
        }
        FGStorageManager.getInstance().unmarkForDeletion(region);
        FGStorageManager.getInstance().updateRegion(region);
        return true;
    }

    public IRegion getRegion(World world, String name) {
        ReadWriteLock lock = this.regionLocks.get(world);
        try {
            lock.readLock().lock();
            for (IRegion region : this.regions.get(world)) {
                if (region.getName().equalsIgnoreCase(name)) {
                    return region;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public IRegion getRegion(Server server, String worldName, String regionName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() ? this.getRegion(world.get(), regionName) : null;
    }

    public Stream<IRegion> getRegionListAsStream(World world) {
        return this.getRegionsListCopy(world).stream();
    }

    public List<IRegion> getRegionsListCopy() {
        List<IRegion> list = new LinkedList<>();
        this.regions.forEach((world, tlist) -> {
            ReadWriteLock lock = this.regionLocks.get(world);
            try {
                lock.readLock().lock();
                tlist.forEach(list::add);
            } finally {
                lock.readLock().unlock();
            }
        });
        return list;
    }

    public List<IRegion> getRegionsListCopy(World world) {
        ReadWriteLock lock = this.regionLocks.get(world);
        try {
            lock.readLock().lock();
            return ImmutableList.copyOf(this.regions.get(world));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<IHandler> getHandlerListCopy() {
        try {
            this.handlerLock.readLock().lock();
            return ImmutableList.copyOf(this.handlers);
        } finally {
            this.handlerLock.readLock().unlock();
        }
    }

    public boolean addHandler(IHandler handler) {
        if (handler == null) return false;
        if (gethandler(handler.getName()) != null) return false;
        try {
            this.handlerLock.writeLock().lock();
            handlers.add(handler);
        } finally {
            this.handlerLock.writeLock().unlock();
        }
        FGStorageManager.getInstance().unmarkForDeletion(handler);
        FGStorageManager.getInstance().updateHandler(handler);
        return true;
    }

    public IHandler gethandler(String name) {
        try {
            this.handlerLock.readLock().lock();
            for (IHandler handler : handlers) {
                if (handler.getName().equalsIgnoreCase(name)) {
                    return handler;
                }
            }
        } finally {
            this.handlerLock.readLock().unlock();
        }
        return null;
    }

    public boolean removeHandler(String name) {
        return this.removeHandler(gethandler(name));
    }

    public boolean removeHandler(IHandler handler) {
        if (handler == null || handler instanceof GlobalHandler) return false;
        this.regions.forEach((world, list) -> {
            ReadWriteLock lock = this.regionLocks.get(world);
            try {
                lock.readLock().lock();
                list.stream()
                        .filter(region -> region.getHandlersCopy().contains(handler))
                        .forEach(region -> region.removeHandler(handler));
            } finally {
                lock.readLock().unlock();
            }
        });
        try {
            this.handlerLock.writeLock().lock();
            if (!this.handlers.contains(handler)) {
                return false;
            }
            FGStorageManager.getInstance().markForDeletion(handler);
            handlers.remove(handler);
            return true;
        } finally {
            this.handlerLock.writeLock().unlock();
        }
    }

    public boolean removeRegion(World world, String name) {
        return this.removeRegion(world, getRegion(world, name));
    }

    public void removeRegion(IRegion region) {
        if (region.getWorld() != null) removeRegion(region.getWorld(), region);
        else this.regions.keySet().forEach((world) -> this.removeRegion(world, region));
    }

    public boolean removeRegion(World world, IRegion region) {
        if (region == null || region instanceof GlobalRegion) return false;
        ReadWriteLock lock = this.regionLocks.get(world);
        try {
            lock.writeLock().lock();
            if (!this.regions.get(world).contains(region)) {
                return false;
            }
            FGStorageManager.getInstance().markForDeletion(region);
            this.regions.get(world).remove(region);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
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
        if (region == null || handler == null || region.getHandlersCopy().contains(handler)) return false;
        if (handler instanceof GlobalHandler && !(region instanceof GlobalRegion)) return false;
        return region.addHandler(handler);
    }

    public boolean unlink(Server server, String worldName, String regionName, String handlerName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.unlink(world.get(), regionName, handlerName);
    }

    public boolean unlink(World world, String regionName, String handlerName) {
        IRegion region = getRegion(world, regionName);
        IHandler handler = gethandler(handlerName);
        return this.unlink(region, handler);
    }

    public boolean unlink(IRegion region, IHandler handler) {
        if (region == null || handler == null || !region.getHandlersCopy().contains(handler)) return false;
        if (handler instanceof GlobalHandler && region instanceof GlobalRegion) return false;
        return region.removeHandler(handler);
    }

    public boolean renameRegion(World world, String oldName, String newName) {
        return this.rename(this.getRegion(world, oldName), newName);
    }

    public boolean renameRegion(Server server, String worldName, String oldName, String newName) {
        return this.rename(this.getRegion(server, worldName, oldName), newName);
    }

    public boolean rename(IFGObject object, String newName) {
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
        return false;
    }

    public boolean renameHandler(String oldName, String newName) {
        return this.rename(this.gethandler(oldName), newName);
    }

    public void createLists(World world) {
        regions.put(world, new LinkedList<>());
    }

    public void populateWorld(World world) {
        this.createLists(world);
        GlobalRegion gr = new GlobalRegion();
        gr.addHandler(this.globalHandler);
        addRegion(world, gr);
    }

    public GlobalHandler getGlobalHandler() {
        return globalHandler;
    }
}
