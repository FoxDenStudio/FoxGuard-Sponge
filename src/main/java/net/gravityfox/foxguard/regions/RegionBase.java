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

package net.gravityfox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.handlers.IHandler;
import org.spongepowered.api.world.World;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public abstract class RegionBase implements IRegion {

    protected final List<IHandler> handlers;
    protected String name;
    protected World world;
    protected boolean isEnabled = true;

    public RegionBase(String name) {
        this.name = name;
        this.handlers = new LinkedList<>();
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
        return this.handlers;
    }

    @Override
    public boolean addHandler(IHandler handler) {
        if (handlers.contains(handler) || !FGManager.getInstance().isRegistered(handler)) return false;
        this.handlers.add(handler);
        return true;
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        if (!handlers.contains(handler)) return false;
        this.handlers.remove(handler);
        return true;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public void setWorld(World world) {
        if (this.world == null) this.world = world;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = state;
    }

}
