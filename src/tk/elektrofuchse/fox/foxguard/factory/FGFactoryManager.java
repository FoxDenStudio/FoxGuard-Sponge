/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fox on 10/22/2015.
 * Project: foxguard
 */
public class FGFactoryManager {

    private final List<IRegionFactory> regionFactories;
    private final List<IFlagSetFactory> flagSetFactories;


    private static FGFactoryManager ourInstance = new FGFactoryManager();

    public static FGFactoryManager getInstance() {
        return ourInstance;
    }

    private FGFactoryManager() {
        regionFactories = new ArrayList<>();
        regionFactories.add(new FGRegionFactory());
        flagSetFactories = new ArrayList<>();
        flagSetFactories.add(new FGFlagSetFactory());
    }


    public IRegion createRegion(String name, String type, String args, InternalCommandState state, World world, CommandSource source) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (FGHelper.contains(rf.getAliases(), type)) {
                IRegion region = rf.createRegion(name, type, args, state, world, source);
                if (region != null) return region;
            }
        }
        return null;
    }

    public IRegion createRegion(DataSource source, String name, String type) throws SQLException {
        for (IRegionFactory rf : regionFactories) {
            if (FGHelper.contains(rf.getTypes(), type)) {
                IRegion region = rf.createRegion(source, name, type);
                if (region != null) return region;
            }
        }
        return null;
    }


    public IFlagSet createFlagSet(String name, String type, int priority, String args, InternalCommandState state, CommandSource source) {
        for (IFlagSetFactory fsf : flagSetFactories) {
            if (FGHelper.contains(fsf.getAliases(), type)) {
                IFlagSet flagSet = fsf.createFlagSet(name, type, priority, args, state, source);
                if (flagSet != null) return flagSet;
            }
        }
        return null;
    }

    public IFlagSet createFlagSet(DataSource source, String name, String type, int priority) throws SQLException {
        for (IFlagSetFactory fsf : flagSetFactories) {
            if (FGHelper.contains(fsf.getTypes(), type)) {
                IFlagSet flagSet = fsf.createFlagSet(source, name, type, priority);
                if (flagSet != null) return flagSet;
            }
        }
        return null;
    }

    public boolean registerRegionFactory(IRegionFactory factory) {
        if (regionFactories.contains(factory)) return false;
        regionFactories.add(factory);
        return true;
    }

    public boolean registerFlagSetFactory(IFlagSetFactory factory) {
        if (flagSetFactories.contains(factory)) return false;
        flagSetFactories.add(factory);
        return true;
    }

    public boolean unregister(Object factory) {
        if (factory instanceof IRegionFactory)
            return regionFactories.remove(factory);
        else if (factory instanceof IFlagSetFactory)
            return flagSetFactories.remove(factory);
        return false;
    }
}
