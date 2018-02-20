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
import net.foxdenstudio.sponge.foxguard.plugin.handler.HandlerData;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class ControllerBase extends HandlerBase implements IController {

    protected final List<IHandler> links;

    public ControllerBase(HandlerData data) {
        super(data);
        this.links = new ArrayList<>();
    }

    @Override
    public List<IHandler> getLinks() {
        return ImmutableList.copyOf(this.links);
    }

    @Override
    public boolean addLink(IHandler handler) {
        if (!FGManager.getInstance().isRegistered(handler)) return false;
        int maxLinks = this.maxLinks();
        return !(maxLinks >= 0 && this.links.size() >= maxLinks) && this.links.add(handler);
    }

    @Override
    public boolean removeLink(IHandler handler) {
        return this.links.remove(handler);
    }

    @Override
    public void clearLinks() {
        this.links.clear();
    }

    /**
     * Called when links are to be loaded. The new FoxGuard storage manager stores and loads links for you, but
     *
     * @param directory
     * @param savedList
     */
    @Override
    public void loadLinks(Path directory, List<IHandler> savedList) {
        links.clear();
        links.addAll(savedList);
    }

    /**
     * Stub method for controllers to put custom link saving code. This way, subclasses have something to call to save only links.
     * I'm not sure why this is useful, if it's useful at all. I might end up removing it, or redoing controller save code entirely,
     * cause array based serialization makes much less sense for controllers than regions.
     * <p>
     * so actually, while i'm at it:
     * TODO do something like map or arbitrary tree based serialization instea for controllers in a way that foxguard storage understands.
     *
     * @param directory the save directory of the controller
     */
    protected void saveLinks(Path directory) {

    }
}
