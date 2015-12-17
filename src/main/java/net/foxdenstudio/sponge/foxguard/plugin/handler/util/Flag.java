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

import org.spongepowered.api.util.Tristate;

public enum Flag {
    ROOT(null, true, "root", "Everything"),

    BLOCK(ROOT, true, "block", "Blocks"),
    BLOCK_PLACE(BLOCK, true, "blockplace", "Place-Blocks"),
    BLOCK_BREAK(BLOCK, true, "blockbreak", "Break-Blocks"),
    BLOCK_MODIFY(BLOCK, true, "blockmodify", "Modify-Blocks"),
    BLOCK_INTERACT(BLOCK, true, "blockclick", "Click-Blocks"),
    BLOCK_INTERACT_PRIMARY(BLOCK_INTERACT, true, "blockattack", "Attack-Blocks"),
    BLOCK_INTERACT_SECONDARY(BLOCK_INTERACT, true, "blockinteract", "Interact-Blocks"),
    ENTITY_INTERACT(ROOT, true, "entityclick", "Click-Entities"),
    ENTITY_INTERACT_PRIMARY(ENTITY_INTERACT, true, "entityattack", "Attack-Entities"),
    ENTITY_INTERACT_SECONDARY(ENTITY_INTERACT, true, "entityinteract", "Interact-Entities"),
    BLOCK_FLUID(BLOCK, true, "fluids", "Fluids"),
    PLAYER_INTERACT_PRIMARY(ENTITY_INTERACT_PRIMARY, true, "playerattack", "Attack-Player"),
    SPAWN_MOB(ROOT, true, "spawnmob", "Spawn-Mobs"),
    SPAWN_MOB_HOSTILE(SPAWN_MOB, true, "spawnmobhostile", "Spawn-Hostile-Mobs"),
    SPAWN_MOB_PASSIVE(SPAWN_MOB, true, "spawnmobpassive", "Spawn-Passive-Mobs");

    final String humanName;
    final String flagName;
    final boolean defaultValue;

    Flag parent = null;

    Flag(Flag parent, boolean defaultValue, String flagName, String humanName) {
        this.parent = parent;
        this.defaultValue = defaultValue;
        this.flagName = flagName;
        this.humanName = humanName;
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

    public Tristate resolve(Tristate input) {
        if (input == Tristate.UNDEFINED) return this.defaultValue ? Tristate.TRUE : Tristate.FALSE;
        else return input;
    }
}
