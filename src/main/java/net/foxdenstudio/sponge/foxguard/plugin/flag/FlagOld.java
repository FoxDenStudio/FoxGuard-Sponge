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
import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.util.Tristate;

import java.util.*;

public enum FlagOld implements IFlag {

    ROOT(true, "root", "Root"),
    BUFF(false, "buff", "Buffs", ROOT),
    DEBUFF(true, "debuff", "Debuffs", ROOT),

    INTERACT(true, "click", "Click", DEBUFF),
    INTERACT_PRIMARY(true, "attack", "Attack", INTERACT),
    INTERACT_SECONDARY(true, "interact", "Interact", INTERACT),

    BLOCK(true, "block", "Blocks", DEBUFF),
    BLOCK_CHANGE(true, "blockchange", "Change-Blocks", BLOCK),
    BLOCK_PLACE(true, "blockplace", "Place-Blocks", BLOCK_CHANGE),
    BLOCK_BREAK(true, "blockbreak", "Break-Blocks", BLOCK_CHANGE),
    BLOCK_MODIFY(true, "blockmodify", "Modify-Blocks", BLOCK_CHANGE),
    BLOCK_DECAY(true, "blockdecay", "Block-Decay", BLOCK_CHANGE),
    BLOCK_GROW(true, "blockgrow", "Block-Growth", BLOCK_CHANGE),

    BLOCK_INTERACT(true, "blockclick", "Click-Blocks", INTERACT, BLOCK),
    BLOCK_INTERACT_PRIMARY(true, "blockattack", "Attack-Blocks", BLOCK_INTERACT, INTERACT_PRIMARY),
    BLOCK_INTERACT_SECONDARY(true, "blockinteract", "Interact-Blocks", BLOCK_INTERACT, INTERACT_SECONDARY),

    ENTITY_INTERACT(true, "entityclick", "Click-Entities", INTERACT),
    ENTITY_INTERACT_PRIMARY(true, "entityattack", "Attack-Entities", ENTITY_INTERACT, INTERACT_PRIMARY),
    ENTITY_INTERACT_SECONDARY(true, "entityinteract", "Interact-Entities", ENTITY_INTERACT, INTERACT_SECONDARY),

    PLAYER_INTERACT(true, "playerclick", "Click-Players", ENTITY_INTERACT),
    PLAYER_INTERACT_PRIMARY(true, "playerattack", "Attack-Players", PLAYER_INTERACT, ENTITY_INTERACT_PRIMARY),
    PLAYER_INTERACT_SECONDARY(true, "playerinteract", "Interact-Players", PLAYER_INTERACT, ENTITY_INTERACT_SECONDARY),

    SPAWN_MOB(true, "spawnmob", "Spawn-Mobs", DEBUFF),
    SPAWN_MOB_PASSIVE(true, "spawnmobpassive", "Spawn-Passive-Mobs", SPAWN_MOB),
    SPAWN_MOB_HOSTILE(true, "spawnmobhostile", "Spawn-Hostile-Mobs", SPAWN_MOB),

    PLAYER_PASS(true, "playerpass", "Player-Pass-Borders", DEBUFF),
    PLAYER_ENTER(true, "playerenter", "Player-Enter", PLAYER_PASS),
    PLAYER_EXIT(true, "playerexit", "Player-Exit", PLAYER_PASS),

    DAMAGE_ENTITY(true, "damageentity", "Damage-Entities", DEBUFF),
    DAMAGE_LIVING(true, "damageliving", "Damage-Living", DAMAGE_ENTITY),
    DAMAGE_MOB(true, "damagemob", "Damage-Mobs", DAMAGE_LIVING),
    DAMAGE_MOB_PASSIVE(true, "damagemobpassive", "Damage-Passive-Mobs", DAMAGE_MOB),
    DAMAGE_MOB_HOSTILE(true, "damagemobhostile", "Damage-Hostile-Mobs", DAMAGE_MOB),
    DAMAGE_PLAYER(true, "damageplayer", "Damage-Players", DAMAGE_LIVING),

    KILL_LIVING(true, "killliving", "Kill-Living", DAMAGE_LIVING),
    KILL_MOB(true, "killmob", "Kill-Mobs", KILL_LIVING, DAMAGE_MOB),
    KILL_MOB_PASSIVE(true, "killmobpassive", "Kill-Passive-Mobs", KILL_MOB, DAMAGE_MOB_PASSIVE),
    KILL_MOB_HOSTILE(true, "killmobhostile", "Kill-Hostile-Mobs", KILL_MOB, DAMAGE_MOB_HOSTILE),
    KILL_PLAYER(true, "killplayer", "Kill-Players", KILL_LIVING, DAMAGE_PLAYER),

    IGNITE_ENTITY(true, "igniteentity", "Ignite-Entities", DAMAGE_ENTITY),
    IGNITE_LIVING(true, "igniteliving", "Ignite-Living", IGNITE_ENTITY, DAMAGE_LIVING),
    IGNITE_MOB(true, "ignitemob", "Ignite-Mobs", IGNITE_LIVING, DAMAGE_MOB),
    IGNITE_MOB_PASSIVE(true, "ignitemobpassive", "Ignite-Passive-Mobs", IGNITE_MOB, DAMAGE_MOB_PASSIVE),
    IGNITE_MOB_HOSTILE(true, "ignitemobhostile", "Ignite-Hostile-Mobs", IGNITE_MOB, DAMAGE_MOB_HOSTILE),
    IGNITE_PLAYER(true, "igniteplayer", "Ignite-Players", IGNITE_LIVING, DAMAGE_PLAYER),

    EXPLOSION(true, "explosion", "Explosions", DEBUFF),

    INVINCIBLE(false, "invincible", "Invincibility", BUFF),
    UNDYING(false, "undying", "Undying", INVINCIBLE);


    private final String humanName;
    private final String flagName;
    private final boolean defaultValue;
    private final FlagOld[] parents;

    private List<Set<IFlag>> hierarchy;


    FlagOld(boolean defaultValue, String flagName, String humanName, FlagOld... parents) {
        this.parents = parents;
        this.defaultValue = defaultValue;
        this.flagName = flagName;
        this.humanName = humanName;
    }

    public static IFlag flagFrom(String name) {
        for (FlagOld flag : values()) {
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
    private static List<IFlag> allFlags = null;

    public static List<IFlag> getFlags() {
        if (allFlags == null) genFlagsList();
        return allFlags;
    }

    private static void genFlagsList() {
        ImmutableList.Builder<IFlag> builder = ImmutableList.builder();
        builder.addAll(Arrays.asList(values()));
        builder.addAll(otherFlags);
        allFlags = builder.build();
    }

    public static boolean addFlag(IFlag flag) {
        if (flag != null && flagFrom(flag.flagName()) == null) {
            otherFlags.add(flag);
            genFlagsList();
            return true;
        } else return false;
    }

    public static void addAllFlags(Iterable<IFlag> flags) {
        if (flags == null) return;
        for (IFlag flag : flags) {
            addFlag(flag);
        }
        genFlagsList();
    }

    public static final Comparator<IFlag> COMPARATOR = (f1, f2) -> {
        if (f1 instanceof FlagOld) {
            if (f2 instanceof FlagOld) {
                return ((FlagOld) f1).compareTo((FlagOld) f2);
            } else {
                return -1;
            }
        } else {
            if (f2 instanceof FlagOld) {
                return 1;
            } else {
                return otherFlags.indexOf(f1) - otherFlags.indexOf(f2);
            }
        }
    };
}
