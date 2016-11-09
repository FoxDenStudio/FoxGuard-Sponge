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

/**
 * Created by Fox on 5/25/2016.
 */
public class Flags {

    private static final FlagRegistry REGISTRY = FlagRegistry.getInstanceInternal();

    public static final Flag ROOT = REGISTRY.registerFlag("root");
    public static final Flag BUFF = REGISTRY.registerFlag("buff");
    public static final Flag DEBUFF = REGISTRY.registerFlag("debuff");

    public static final Flag INTERACT = REGISTRY.registerFlag("interact");
    public static final Flag MAIN = REGISTRY.registerFlag("main");
    public static final Flag OFF = REGISTRY.registerFlag("off");
    public static final Flag PRIMARY = REGISTRY.registerFlag("primary");
    public static final Flag SECONDARY = REGISTRY.registerFlag("secondary");

    public static final Flag BLOCK = REGISTRY.registerFlag("block");
    public static final Flag CHANGE = REGISTRY.registerFlag("change");
    public static final Flag PLACE = REGISTRY.registerFlag("place");
    public static final Flag BREAK = REGISTRY.registerFlag("break");
    public static final Flag MODIFY = REGISTRY.registerFlag("modify");
    public static final Flag DECAY = REGISTRY.registerFlag("decay");
    public static final Flag GROW = REGISTRY.registerFlag("grow");
    public static final Flag POST = REGISTRY.registerFlag("post");
    public static final Flag EXPLOSION = REGISTRY.registerFlag("explosion");

    public static final Flag DAMAGE = REGISTRY.registerFlag("damage");
    public static final Flag KILL = REGISTRY.registerFlag("kill");
    //public static final Flag IGNITE = REGISTRY.registerFlag("ignite");
    public static final Flag SPAWN = REGISTRY.registerFlag("spawn");

    public static final Flag ENTITY = REGISTRY.registerFlag("entity");
    public static final Flag HANGING = REGISTRY.registerFlag("hanging");
    public static final Flag LIVING = REGISTRY.registerFlag("living");
    public static final Flag MOB = REGISTRY.registerFlag("mob");
    public static final Flag PASSIVE = REGISTRY.registerFlag("passive");
    public static final Flag HOSTILE = REGISTRY.registerFlag("hostile");
    public static final Flag HUMAN = REGISTRY.registerFlag("human");
    public static final Flag PLAYER = REGISTRY.registerFlag("player");

    public static final Flag MOVE = REGISTRY.registerFlag("move");
    public static final Flag TELEPORT = REGISTRY.registerFlag("teleport");
    public static final Flag PORTAL = REGISTRY.registerFlag("portal");
    public static final Flag ENTER = REGISTRY.registerFlag("enter");
    public static final Flag EXIT = REGISTRY.registerFlag("exit");

    public static final Flag INVINCIBLE = REGISTRY.registerFlag("invincible");
    public static final Flag UNDYING = REGISTRY.registerFlag("undying");

}
