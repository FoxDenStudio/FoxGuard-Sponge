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

package net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by Fox on 5/25/2016.
 */
public class FlagSet {

    private static final FlagRegistry FLAG_REGISTRY = FlagRegistry.getInstance();

    private final boolean[] flags;
    private int hash;

    public FlagSet(boolean[] flags) {
        this.flags = flags;
        this.hash = Arrays.hashCode(flags);
    }

    public static boolean[] arrayFromFlags(Flag... flags) {
        boolean[] array = new boolean[FLAG_REGISTRY.getNumFlags()];
        for (Flag flag : flags) {
            array[flag.id] = true;
        }
        return array;
    }

    public boolean get(Flag flag) {
        return this.flags[flag.id];
    }

    public boolean get(int flag) {
        return this.flags[flag];
    }

    public Set<Flag> toFlagSet() {
        ImmutableSet.Builder<Flag> builder = ImmutableSet.builder();
        for (int i = 0; i < flags.length; i++) {
            if (this.flags[i]) builder.add(FLAG_REGISTRY.getFlag(i));
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.hash != o.hashCode()) return false;
        FlagSet flagSet = (FlagSet) o;
        return Arrays.equals(flags, flagSet.flags);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    protected FlagSet clone() {
        return new FlagSet(this.flags.clone());
    }
}
