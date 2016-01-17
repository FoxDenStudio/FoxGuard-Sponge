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

import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;

public final class EventResult {

    private static final EventResult SUCCESS = of(Tristate.TRUE);
    private static final EventResult PASSTHROUGH = of(Tristate.UNDEFINED);
    private static final EventResult FAILURE = of(Tristate.FALSE);

    private final Tristate state;
    private final Optional<Text> message;

    private EventResult(Tristate success, Optional<Text> message) {
        this.state = success;
        this.message = message;
    }

    public static EventResult of(Tristate state) {
        return new EventResult(state, Optional.empty());
    }

    public static EventResult of(Tristate success, Text message) {
        return new EventResult(success, Optional.of(message));
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

    public Optional<Text> getMessage() {
        return message;
    }
}
