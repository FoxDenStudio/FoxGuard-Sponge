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

import com.google.common.collect.ImmutableSet;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectBase;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectData;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import net.foxdenstudio.sponge.foxguard.plugin.util.RegionCache;

import java.util.HashSet;
import java.util.Set;

public abstract class RegionBase extends FGObjectBase implements IRegion {

    private final Set<IHandler> handlers;

    protected RegionBase(FGObjectData data) {
        super(data);
        this.handlers = new HashSet<>();
    }

    @Override
    public void setEnabled(boolean state) {
        super.setEnabled(state);
        FGManager.getInstance().markDirty(this, RegionCache.DirtyType.MODIFIED);
    }

    @Override
    public Set<IHandler> getLinks() {
        return ImmutableSet.copyOf(this.handlers);
    }

    @Override
    public boolean addLink(IHandler handler) {
        return FGManager.getInstance().isRegistered(handler) && this.handlers.add(handler);
    }

    @Override
    public boolean removeLink(IHandler handler) {
        return this.handlers.remove(handler);
    }

    @Override
    public void clearLinks() {
        this.handlers.clear();
    }

    public void markDirty() {
        FGUtil.markDirty(this);
    }

}
