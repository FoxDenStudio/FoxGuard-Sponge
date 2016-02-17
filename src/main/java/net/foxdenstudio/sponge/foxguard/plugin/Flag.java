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

package net.foxdenstudio.sponge.foxguard.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.util.Tristate;

import java.util.*;

public enum Flag {

    ROOT(true, "root", "Everything"),

    BLOCK(true, "block", "Blocks", ROOT),
    BLOCK_CHANGE(true, "blockchange", "Change-Blocks", BLOCK),
    BLOCK_PLACE(true, "blockplace", "Place-Blocks", BLOCK_CHANGE),
    BLOCK_BREAK(true, "blockbreak", "Break-Blocks", BLOCK_CHANGE),
    BLOCK_MODIFY(true, "blockmodify", "Modify-Blocks", BLOCK_CHANGE),
    BLOCK_INTERACT(true, "blockclick", "Click-Blocks", BLOCK),
    BLOCK_INTERACT_PRIMARY(true, "blockattack", "Attack-Blocks", BLOCK_INTERACT),
    BLOCK_INTERACT_SECONDARY(true, "blockinteract", "Interact-Blocks", BLOCK_INTERACT),

    ENTITY_INTERACT(true, "entityclick", "Click-Entities", ROOT),
    ENTITY_INTERACT_PRIMARY(true, "entityattack", "Attack-Entities", ENTITY_INTERACT),
    ENTITY_INTERACT_SECONDARY(true, "entityinteract", "Interact-Entities", ENTITY_INTERACT),
    PLAYER_INTERACT_PRIMARY(true, "playerattack", "Attack-Player", ENTITY_INTERACT_PRIMARY),

    SPAWN_MOB(true, "spawnmob", "Spawn-Mobs", ROOT),
    SPAWN_MOB_HOSTILE(true, "spawnmobhostile", "Spawn-Hostile-Mobs", SPAWN_MOB),
    SPAWN_MOB_PASSIVE(true, "spawnmobpassive", "Spawn-Passive-Mobs", SPAWN_MOB),

    PLAYER_PASS(true, "playerpass", "Player-Pass-Borders", ROOT),
    PLAYER_ENTER(true, "playerenter", "Player-Enter", PLAYER_PASS),
    PLAYER_EXIT(true, "playerexit", "Player-Exit", PLAYER_PASS);


    private final String humanName;
    private final String flagName;
    private final boolean defaultValue;
    private final Flag[] parents;

    private List<Set<Flag>> hiearchy;


    Flag(boolean defaultValue, String flagName, String humanName, Flag... parents) {
        this.parents = parents;
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
        return parents.length != 0;
    }

    public Flag[] getParents() {
        return parents;
    }

    public List<Set<Flag>> getHiearchy(){
        if (hiearchy == null) {
            List<Set<Flag>> list = new ArrayList<>();
            Set<Flag> current = new LinkedHashSet<>();
            current.add(this);
            while (!current.isEmpty()){
                Set<Flag> newSet = new HashSet<>();
                current.forEach(flag -> Arrays.stream(flag.getParents()).forEach(newSet::add));
                list.add(ImmutableSet.copyOf(current));
                current = newSet;
            }
            hiearchy = ImmutableList.copyOf(list);
        }
        return hiearchy;
    }

    public Tristate resolve(Tristate input) {
        if (input == Tristate.UNDEFINED) return this.defaultValue ? Tristate.TRUE : Tristate.FALSE;
        else return input;
    }
}
