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

package net.foxdenstudio.sponge.foxguard.pluginold.handler.util;

import net.foxdenstudio.sponge.foxguard.pluginold.flag.Flag;
import net.foxdenstudio.sponge.foxguard.pluginold.flag.FlagRegistry;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Fox on 11/15/2016.
 */
public class TristateEntry extends Entry {

    public Tristate tristate;

    public TristateEntry(Set<Flag> set, Tristate tristate) {
        super(set);
        this.tristate = tristate;
    }

    public TristateEntry(Tristate tristate, Flag... flags) {
        super(flags);
        this.tristate = tristate;
    }

    public String serializeValue(){
        return tristate.name();
    }

    public static TristateEntry deserialize(String string) {
        FlagRegistry registry = FlagRegistry.getInstance();
        String[] parts = string.split(":");
        String[] flags = parts[0].split(",");
        Set<Flag> flagSet = new HashSet<>();
        for (String flagName : flags) {
            Optional<Flag> flagOptional = registry.getFlag(flagName);
            if (flagOptional.isPresent()) {
                flagSet.add(flagOptional.get());
            }
        }
        return new TristateEntry(flagSet, Tristate.valueOf(parts[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        TristateEntry that = (TristateEntry) o;
        return tristate == that.tristate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tristate);
    }
}
