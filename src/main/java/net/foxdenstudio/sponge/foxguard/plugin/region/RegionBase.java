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

package net.foxdenstudio.sponge.foxguard.plugin.region;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGObjectBase;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;

public abstract class RegionBase extends FGObjectBase implements IRegion {

    private final List<IHandler> handlers;
    private World world;

    RegionBase(String name) {
        super(name);
        this.handlers = new ArrayList<>();
    }

    @Override
    public boolean isInRegion(Vector3i vec) {
        return this.isInRegion(vec.getX(), vec.getY(), vec.getZ());
    }

    @Override
    public boolean isInRegion(Vector3d vec) {
        return this.isInRegion(vec.getX(), vec.getY(), vec.getZ());
    }

    @Override
    public List<IHandler> getHandlers() {
        try {
            this.lock.readLock().lock();
            return ImmutableList.copyOf(this.handlers);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean addHandler(IHandler handler) {
        if (!FGManager.getInstance().isRegistered(handler)) {
            return false;
        }
        try {
            this.lock.writeLock().lock();
            return this.handlers.add(handler);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        try {
            this.lock.writeLock().lock();
            return this.handlers.remove(handler);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public World getWorld() {
        try {
            this.lock.readLock().lock();
            return this.world;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void setWorld(World world) {
        try {
            this.lock.writeLock().lock();
            if (this.world == null) {
                this.world = world;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

}
