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

public enum Flag implements IFlag {

    ROOT(true, "root", "Everything"),

    INTERACT(true, "click", "Click", ROOT),
    INTERACT_PRIMARY(true, "attack", "Attack", INTERACT),
    INTERACT_SECONDARY(true, "interact", "Interact", INTERACT),

    BLOCK(true, "block", "Blocks", ROOT),
    BLOCK_CHANGE(true, "blockchange", "Change-Blocks", BLOCK),
    BLOCK_PLACE(true, "blockplace", "Place-Blocks", BLOCK_CHANGE),
    BLOCK_BREAK(true, "blockbreak", "Break-Blocks", BLOCK_CHANGE),
    BLOCK_MODIFY(true, "blockmodify", "Modify-Blocks", BLOCK_CHANGE),

    BLOCK_INTERACT(true, "blockclick", "Click-Blocks", INTERACT),
    BLOCK_INTERACT_PRIMARY(true, "blockattack", "Attack-Blocks", BLOCK_INTERACT, INTERACT_PRIMARY),
    BLOCK_INTERACT_SECONDARY(true, "blockinteract", "Interact-Blocks", BLOCK_INTERACT, INTERACT_SECONDARY),

    ENTITY_INTERACT(true, "entityclick", "Click-Entities", INTERACT),
    ENTITY_INTERACT_PRIMARY(true, "entityattack", "Attack-Entities", ENTITY_INTERACT, INTERACT_PRIMARY),
    ENTITY_INTERACT_SECONDARY(true, "entityinteract", "Interact-Entities", ENTITY_INTERACT, INTERACT_SECONDARY),

    PLAYER_INTERACT(true, "playerclick", "Click-Players", ENTITY_INTERACT),
    PLAYER_INTERACT_PRIMARY(true, "playerattack", "Attack-Players", PLAYER_INTERACT, ENTITY_INTERACT_PRIMARY),
    PLAYER_INTERACT_SECONDARY(true, "playerinteract", "Interact-Players", PLAYER_INTERACT, ENTITY_INTERACT_SECONDARY),

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

    private List<Set<IFlag>> hierarchy;


    Flag(boolean defaultValue, String flagName, String humanName, Flag... parents) {
        this.parents = parents;
        this.defaultValue = defaultValue;
        this.flagName = flagName;
        this.humanName = humanName;
    }

    public static IFlag flagFrom(String name) {
        for (Flag flag : Flag.values()) {
            if (flag.flagName.equalsIgnoreCase(name)) return flag;
        }
        for (IFlag flag : otherFlags) {
            if (flag.flagName().equalsIgnoreCase(name)) return flag;
        }
        return null;
    }

    @Override
    public String toString() {
        return humanName;
    }

    @Override
    public String flagName() {
        return flagName;
    }

    @Override
    public IFlag[] getParents() {
        return parents;
    }

    @Override
    public List<Set<IFlag>> getHierarchy() {
        if (hierarchy == null) {
            List<Set<IFlag>> list = new ArrayList<>();
            Set<IFlag> current = new LinkedHashSet<>();
            current.add(this);
            while (!current.isEmpty()) {
                list.add(ImmutableSet.copyOf(current));
                Set<IFlag> newSet = new LinkedHashSet<>();
                current.forEach(flag -> Arrays.stream(flag.getParents()).forEach(newSet::add));
                current = newSet;
            }
            hierarchy = ImmutableList.copyOf(list);
        }
        return hierarchy;
    }

    /**
     * This method is a convenience method for other plugins who wish to add more flags.
     * When implementing {@link IFlag#getHierarchy()}, the behavior is extremely important.
     * As such they can simply call this method, which will handle all of the logic for them.
     * Of course they should cache the result themselves, as it will never change. Caching it here would be slower.
     *
     * @param child
     * @return
     */
    public static List<Set<IFlag>> getHierarchyStatic(IFlag child) {
        List<Set<IFlag>> list = new ArrayList<>();
        Set<IFlag> current = new LinkedHashSet<>();
        current.add(child);
        while (!current.isEmpty()) {
            list.add(ImmutableSet.copyOf(current));
            Set<IFlag> newSet = new LinkedHashSet<>();
            current.forEach(flag -> Arrays.stream(flag.getParents()).forEach(newSet::add));
            current = newSet;
        }
        return ImmutableList.copyOf(list);
    }

    @Override
    public Tristate resolve(Tristate input) {
        if (input == Tristate.UNDEFINED) return this.defaultValue ? Tristate.TRUE : Tristate.FALSE;
        else return input;
    }

    private static List<IFlag> otherFlags = new ArrayList<>();

    public static boolean addFlag(IFlag flag) {
        if (flagFrom(flag.flagName()) == null) {
            otherFlags.add(flag);
            return true;
        } else return false;
    }

    public static void addAllFlags(Iterable<IFlag> flags) {
        for (IFlag flag : flags) {
            addFlag(flag);
        }
    }
}
