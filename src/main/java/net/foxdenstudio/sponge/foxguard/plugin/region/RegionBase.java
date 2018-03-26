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

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectBase;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import net.foxdenstudio.sponge.foxguard.plugin.util.RegionCache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class RegionBase extends FGObjectBase implements IRegion {

    private final Set<IHandler> handlers;

    protected RegionBase(String name, boolean isEnabled) {
        super(name, isEnabled);
        this.handlers = new HashSet<>();
    }

    @Override
    public void setIsEnabled(boolean state) {
        super.setIsEnabled(state);
        FGManager.getInstance().markDirty(this, RegionCache.DirtyType.MODIFIED);
    }

    @Override
    public List<IHandler> getHandlers() {
        return ImmutableList.copyOf(this.handlers);
    }

    @Override
    public boolean addHandler(IHandler handler) {
        return FGManager.getInstance().isRegistered(handler) && this.handlers.add(handler);
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        return this.handlers.remove(handler);
    }

    @Override
    public void clearHandlers() {
        this.handlers.clear();
    }

    public void markDirty() {
        FGUtil.markRegionDirty(this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                ", isEnabled=" + isEnabled +
                ", type=" + this.getUniqueTypeString() +
                ", links=" + this.handlers.stream().map(IFGObject::getName).collect(Collectors.toList()) +
                '}';
    }

}
