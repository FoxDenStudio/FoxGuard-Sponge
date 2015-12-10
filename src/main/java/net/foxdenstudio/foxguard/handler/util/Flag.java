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

package net.foxdenstudio.foxguard.handler.util;

public enum Flag {
    ALL("all", "Everything"),

    BLOCK("block", "Blocks", ALL),
    BLOCK_PLACE("blockplace", "Place-Blocks", BLOCK),
    BLOCK_BREAK("blockbreak", "Break-Blocks", BLOCK),
    BLOCK_MODIFY("blockmodify", "Modify-Blocks", BLOCK),
    BLOCK_INTERACT("blockclick", "Click-Blocks", BLOCK),
    BLOCK_INTERACT_PRIMARY("blockattack", "Attack-Blocks", BLOCK_INTERACT),
    BLOCK_INTERACT_SECONDARY("blockinteract", "Interact-Blocks", BLOCK_INTERACT),
    ENTITY_INTERACT("entityclick", "Click-Entities", ALL),
    ENTITY_INTERACT_PRIMARY("entityattack", "Attack-Entities", ENTITY_INTERACT),
    ENTITY_INTERACT_SECONDARY("entityinteract", "Interact-Entities", ENTITY_INTERACT),
    BLOCK_FLUID("fluids", "Fluids", BLOCK),
    PLAYER_INTERACT_PRIMARY("playerattack", "Attack-Player", ENTITY_INTERACT_PRIMARY),
    SPAWN_MOB("spawnmob", "Spawn-Mobs", ALL),
    SPAWN_MOB_HOSTILE("spawnmobhostile", "Spawn-Hostile-Mobs", SPAWN_MOB),
    SPAWN_MOB_PASSIVE("spawnmobpassive", "Spawn-Passive-Mobs", SPAWN_MOB);

    String humanName;
    String flagName;

    Flag parent = null;

    Flag(String flagName, String humanName) {
        this.humanName = humanName;
        this.flagName = flagName;
    }

    Flag(String flagName, String humanName, Flag parent) {
        this.humanName = humanName;
        this.flagName = flagName;
        this.parent = parent;
    }

    public static Flag flagFrom(String name) {
        for (Flag flag : Flag.values()) {
            if (flag.flagName.equalsIgnoreCase(name)) return flag;
        }
        return null;
    }

    @Override
    public String toString() {
        return humanName;
    }

    public String flagName() {
        return flagName;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Flag getParent() {
        return parent;
    }
}
