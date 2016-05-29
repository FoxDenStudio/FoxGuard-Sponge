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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Fox on 5/19/2016.
 */
public enum Flag2 implements IFlag2 {
    ROOT("root"),
    BUFF("buff"),
    DEBUFF("debuff"),

    INTERACT("interact"),
    PRIMARY("primary"),
    SECONDARY("secondary"),

    BLOCK("block"),
    CHANGE("change"),
    PLACE("place"),
    BREAK("break"),
    MODIFY("modify"),
    DECAY("decay"),
    GROW("grow"),

    ENTITY("entity"),
    LIVING("living"),
    MOB("mob"),
    PLAYER("player"),

    PASSIVE("passive"),
    HOSTILE("hostile"),

    SPAWN("spawn"),
    PASS("pass"),
    ENTER("enter"),
    EXIT("exit"),
    DAMAGE("damage"),
    KILL("kill"),
    IGNITE("ignite"),
    EXPLOSION("explosion"),
    INVINCIBLE("invincible"),
    UNDYING("undying");

    public final String flagName;

    Flag2(String flagName) {
        this.flagName = flagName;
    }

    public static IFlag2 flagFrom(String name) {
        for (Flag2 flag : values()) {
            if (flag.flagName.equalsIgnoreCase(name)) return flag;
        }
        for (IFlag2 flag : otherFlags) {
            if (flag.flagName().equalsIgnoreCase(name)) return flag;
        }
        return null;
    }

    @Override
    public String flagName() {
        return flagName;
    }

    private static List<IFlag2> otherFlags = new ArrayList<>();
    private static List<IFlag2> allFlags = null;

    public static List<IFlag2> getFlags() {
        if (allFlags == null) genFlagsList();
        return allFlags;
    }

    private static void genFlagsList() {
        ImmutableList.Builder<IFlag2> builder = ImmutableList.builder();
        builder.addAll(Arrays.asList(values()));
        builder.addAll(otherFlags);
        allFlags = builder.build();
    }

    public static boolean addFlag(IFlag2 flag) {
        if (flag != null && flagFrom(flag.flagName()) == null) {
            otherFlags.add(flag);
            genFlagsList();
            return true;
        } else return false;
    }

    public static void addAllFlags(Iterable<IFlag2> flags) {
        if (flags == null) return;
        for (IFlag2 flag : flags) {
            addFlag(flag);
        }
        genFlagsList();
    }

    public static final Comparator<IFlag2> COMPARATOR = (f1, f2) -> {
        if (f1 instanceof Flag2) {
            if (f2 instanceof Flag2) {
                return ((Flag2) f1).compareTo((Flag2) f2);
            } else {
                return -1;
            }
        } else {
            if (f2 instanceof Flag2) {
                return 1;
            } else {
                return otherFlags.indexOf(f1) - otherFlags.indexOf(f2);
            }
        }
    };
}
