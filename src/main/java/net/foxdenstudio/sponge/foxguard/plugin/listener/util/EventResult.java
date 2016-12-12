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

package net.foxdenstudio.sponge.foxguard.plugin.listener.util;

import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import org.spongepowered.api.util.Tristate;

public final class EventResult {

    private static final EventResult SUCCESS = of(Tristate.TRUE);
    private static final EventResult PASSTHROUGH = of(Tristate.UNDEFINED);
    private static final EventResult FAILURE = of(Tristate.FALSE);

    private final Tristate state;
    private final boolean displayDefaultMessage;

    private EventResult(Tristate success, boolean displayDefaultMessage) {
        if(success == null){
            FoxGuardMain.instance().getLogger().warn("Tried to instantiate event result with null tristate! Substituting default value UNDEFINED");
            this.state = Tristate.UNDEFINED;
        } else this.state = success;
        this.displayDefaultMessage = displayDefaultMessage;
    }

    public static EventResult of(Tristate state) {
        return new EventResult(state, true);
    }

    public static EventResult of(Tristate success, boolean displayDefaultMessage) {
        return new EventResult(success, displayDefaultMessage);
    }

    public static EventResult allow() {
        return SUCCESS;
    }

    public static EventResult pass() {
        return PASSTHROUGH;
    }

    public static EventResult deny() {
        return FAILURE;
    }

    public Tristate getState() {
        return state;
    }

    public boolean shouldDisplayDefaultMessage() {
        return displayDefaultMessage;
    }
}
