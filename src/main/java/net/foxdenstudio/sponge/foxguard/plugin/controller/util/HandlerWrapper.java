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

package net.foxdenstudio.sponge.foxguard.plugin.controller.util;

import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;

public class HandlerWrapper {

    public static final HandlerWrapper ALLOW = new HandlerWrapper(Tristate.TRUE);
    public static final HandlerWrapper PASSTHROUGH = new HandlerWrapper(Tristate.UNDEFINED);
    public static final HandlerWrapper DENY = new HandlerWrapper(Tristate.FALSE);

    public final Type type;
    public final Tristate tristate;
    public final IHandler handler;

    private HandlerWrapper(Type type, Tristate tristate, IHandler handler) {
        this.type = type;
        this.tristate = tristate;
        this.handler = handler;
    }

    public HandlerWrapper(IHandler handler) {
        this(Type.WRAPPER, null, handler);
    }

    private HandlerWrapper(Tristate state) {
        this(Type.CONSTANT, state, null);
    }

    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        if (type == Type.WRAPPER) {
            return handler.handle(user, flags, extra);
        } else {
            return EventResult.of(tristate);
        }
    }

    public enum Type {
        CONSTANT, WRAPPER
    }
}
