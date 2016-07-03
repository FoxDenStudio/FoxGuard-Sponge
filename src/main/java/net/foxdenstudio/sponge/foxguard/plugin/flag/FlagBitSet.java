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

package net.foxdenstudio.sponge.foxguard.plugin.flag;

import com.google.common.collect.ImmutableSet;

import java.util.BitSet;
import java.util.Set;

/**
 * Created by Fox on 5/25/2016.
 */
public class FlagBitSet extends BitSet {

    public FlagBitSet() {
        super(FlagRegistry.getInstance().getNumFlags());
    }

    public FlagBitSet(Flag... flags) {
        this();
        for (Flag flag : flags) {
            this.set(flag.id);
        }
    }

    public FlagBitSet(Set<Flag> flags){
        this();
        for (Flag flag : flags) {
            this.set(flag.id);
        }
    }

    public boolean get(Flag flag){
        return this.get(flag.id);
    }

    public void set(Flag flag) {
        this.set(flag.id);
    }

    public void set(Flag flag, boolean value) {
        this.set(flag.id, value);
    }

    public Set<Flag> toFlagSet() {
        ImmutableSet.Builder<Flag> builder = ImmutableSet.builder();
        FlagRegistry registry = FlagRegistry.getInstance();
        int index = -1;
        while((index = this.nextSetBit(index + 1)) >= 0){
            builder.add(registry.getFlag(index));
        }
        return builder.build();
    }
}
