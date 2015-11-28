/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.flagsets.util;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public enum Flags {
    // Active
    BLOCK_PLACE("Place-Blocks"),
    BLOCK_BREAK("Break-Blocks"),
    BLOCK_MODIFY("Modify-Blocks"),
    BLOCK_INTERACT_PRIMARY("Attack-Blocks"),
    BLOCK_INTERACT_SECONDARY("Interact-Blocks"),
    ENTITY_INTERACT_PRIMARY("Attack-Entities"),
    ENTITY_INTERACT_SECONDARY("Interact-Entities"),
    FLUID("Fluids"),
    // Passive
    SPAWN_MOB_HOSTILE("Spawn-Hostile-Mobs"),
    SPAWN_MOB_PASSIVE("Spawn-Passive-Mobs");

    String humanName;

    Flags(String name) {
        this.humanName = name;
    }

    public static Flags flagFrom(String name) {
        if (name.equalsIgnoreCase("blockplace")) {
            return BLOCK_PLACE;
        } else if (name.equalsIgnoreCase("blockbreak")) {
            return BLOCK_BREAK;
        } else if (name.equalsIgnoreCase("blockmodify")) {
            return BLOCK_MODIFY;
        } else if (name.equalsIgnoreCase("blockattack")) {
            return BLOCK_INTERACT_PRIMARY;
        } else if (name.equalsIgnoreCase("blockinteract")) {
            return BLOCK_INTERACT_SECONDARY;
        } else if (name.equalsIgnoreCase("entityattack")) {
            return ENTITY_INTERACT_PRIMARY;
        } else if (name.equalsIgnoreCase("entityinteract")) {
            return ENTITY_INTERACT_SECONDARY;
        } else if (name.equalsIgnoreCase("fluids")) {
            return FLUID;
        } else if (name.equalsIgnoreCase("spawnmobpassive")) {
            return SPAWN_MOB_PASSIVE;
        } else if (name.equalsIgnoreCase("spawnmobhostile")) {
            return SPAWN_MOB_HOSTILE;
        } else return null;

    }

    @Override
    public String toString() {
        return humanName;
    }

}
