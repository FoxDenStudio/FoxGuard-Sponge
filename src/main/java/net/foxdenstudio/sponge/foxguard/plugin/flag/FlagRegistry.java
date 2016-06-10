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

import java.util.*;

/**
 * Created by Fox on 5/25/2016.
 */
public class FlagRegistry {

    private static FlagRegistry ourInstance = new FlagRegistry();

    private final List<FlagObject> flagList = new ArrayList<>();
    private final HashMap<String, FlagObject> flagMap = new HashMap<>();
    private int nextAvailableIndex = 0;

    private FlagRegistry() {
        // Force initialize Flags class.
        // This guarantees that FoxGuard flags are always the first to be registered.
        Flags.ROOT.getClass();
    }

    public static FlagRegistry getInstance() {
        return ourInstance;
    }

    public FlagObject registerFlag(String name){
        for (int i = 0; i < 5 && flagMap.containsKey(name); i++) {
            name += "_";
        }
        if(flagMap.containsKey(name)) return null;
        FlagObject flag = new FlagObject(name, nextAvailableIndex);
        nextAvailableIndex++;
        flagList.add(flag);
        flagMap.put(name, flag);
        return null;
    }

    public FlagObject getFlag(int id){
        return flagList.get(id);
    }

    public FlagObject getFlag(String name){
        return flagMap.get(name);
    }

    public List<FlagObject> getFlagList(){
        return ImmutableList.copyOf(flagList);
    }

    public int getNumFlags(){
        return this.flagList.size();
    }
}
