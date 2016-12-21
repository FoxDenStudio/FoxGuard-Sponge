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

package net.foxdenstudio.sponge.foxguard.plugin.handler.util;

import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Fox on 11/15/2016.
 */
public class PermissionEntry extends Entry {

    public String permission;
    public boolean relative;

    public PermissionEntry(Set<Flag> set, String permission) {
        super(set);
        this.permission = permission.toLowerCase();
    }

    public PermissionEntry(String permission, Flag... flags) {
        super(flags);
        this.permission = permission.toLowerCase();
    }

    public static PermissionEntry deserialize(String string) {
        FlagRegistry registry = FlagRegistry.getInstance();
        String[] parts = string.split(":");
        String[] flags = parts[0].split(",", 2);
        Set<Flag> flagSet = new HashSet<>();
        for (String flagName : flags) {
            Optional<Flag> flagOptional = registry.getFlag(flagName);
            if (flagOptional.isPresent()) {
                flagSet.add(flagOptional.get());
            }
        }
        return new PermissionEntry(flagSet, parts[1]);
    }

    @Override
    public String serializeValue() {
        return permission;
    }
}
