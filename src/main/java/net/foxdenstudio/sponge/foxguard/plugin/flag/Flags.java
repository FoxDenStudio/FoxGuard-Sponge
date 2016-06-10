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

    private static final FlagRegistry REGISTRY = FlagRegistry.getInstance();

    public static final FlagObject ROOT = REGISTRY.registerFlag("root");
    public static final FlagObject BUFF = REGISTRY.registerFlag("buff");
    public static final FlagObject DEBUFF = REGISTRY.registerFlag("debuff");
    public static final FlagObject INTERACT = REGISTRY.registerFlag("interact");
    public static final FlagObject PRIMARY = REGISTRY.registerFlag("primary");
    public static final FlagObject SECONDARY = REGISTRY.registerFlag("secondary");
    public static final FlagObject BLOCK = REGISTRY.registerFlag("block");
    public static final FlagObject CHANGE = REGISTRY.registerFlag("change");
    public static final FlagObject PLACE = REGISTRY.registerFlag("place");
    public static final FlagObject BREAK = REGISTRY.registerFlag("break");
    public static final FlagObject MODIFY = REGISTRY.registerFlag("modify");
    public static final FlagObject DECAY = REGISTRY.registerFlag("decay");
    public static final FlagObject GROW = REGISTRY.registerFlag("grow");
    public static final FlagObject ENTITY = REGISTRY.registerFlag("entity");
    public static final FlagObject LIVING = REGISTRY.registerFlag("living");
    public static final FlagObject MOB = REGISTRY.registerFlag("mob");
    public static final FlagObject PLAYER = REGISTRY.registerFlag("player");
    public static final FlagObject PASSIVE = REGISTRY.registerFlag("passive");
    public static final FlagObject HOSTILE = REGISTRY.registerFlag("hostile");
    public static final FlagObject SPAWN = REGISTRY.registerFlag("spawn");
    public static final FlagObject PASS = REGISTRY.registerFlag("pass");
    public static final FlagObject ENTER = REGISTRY.registerFlag("enter");
    public static final FlagObject EXIT = REGISTRY.registerFlag("exit");
    public static final FlagObject DAMAGE = REGISTRY.registerFlag("damage");
    public static final FlagObject KILL = REGISTRY.registerFlag("kill");
    public static final FlagObject IGNITE = REGISTRY.registerFlag("ignite");
    public static final FlagObject EXPLOSION = REGISTRY.registerFlag("explosion");
    public static final FlagObject INVINCIBLE = REGISTRY.registerFlag("invincible");
    public static final FlagObject UNDYING = REGISTRY.registerFlag("undying");


}
