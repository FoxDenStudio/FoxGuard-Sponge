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

package net.foxdenstudio.sponge.foxguard.plugin.controller;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.HandlerBase;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class ControllerBase extends HandlerBase implements IController {

    protected final List<IHandler> handlers;

    public ControllerBase(String name, boolean isEnabled, int priority) {
        super(name, isEnabled, priority);
        this.handlers = new ArrayList<>();
    }

    @Override
    public List<IHandler> getHandlers() {
        return ImmutableList.copyOf(this.handlers);
    }

    @Override
    public boolean addHandler(IHandler handler) {
        if (!FGManager.getInstance().isRegistered(handler)) return false;
        int maxLinks = this.maxLinks();
        return !(maxLinks >= 0 && this.handlers.size() >= maxLinks) && this.handlers.add(handler);
    }

    @Override
    public boolean removeHandler(IHandler handler) {
        return this.handlers.remove(handler);
    }

    @Override
    public void clearHandlers() {
        this.handlers.clear();
    }

}
