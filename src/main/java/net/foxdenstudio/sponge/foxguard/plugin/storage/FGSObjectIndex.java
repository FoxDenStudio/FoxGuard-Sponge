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

package net.foxdenstudio.sponge.foxguard.plugin.storage;

import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Fox on 7/9/2017.
 * Project: SpongeForge
 */
public class FGSObjectIndex extends FGSObjectMeta {

    boolean enabled;
    Integer priority;
    UUID owner;
    List<String> links;

    public FGSObjectIndex(String name, String category, String type, boolean enabled, Integer priority, UUID owner, List<String> links) {
        super(name, category, type);
        this.enabled = enabled;
        this.priority = priority;
        this.owner = owner;
        this.links = links;
    }

    public FGSObjectIndex() {
    }

    public FGSObjectIndex(IFGObject object) {
        super(object);
        this.enabled = object.isEnabled();
        this.owner = object.getOwner();
        if (object instanceof IHandler) {
            this.priority = ((IHandler) object).getPriority();
        }
        if (object instanceof ILinkable && ((ILinkable) object).saveLinks()) {
            this.links = ((ILinkable) object).getLinks().stream().map(IHandler::getName).collect(Collectors.toList());
        }
    }
}
