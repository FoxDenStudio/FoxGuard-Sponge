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

package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public final class FGFactoryManager {

    private static final FGFactoryManager ourInstance = new FGFactoryManager();
    private final List<IRegionFactory> regionFactories;
    private final List<IHandlerFactory> handlerFactories;

    private FGFactoryManager() {
        regionFactories = new ArrayList<>();
        regionFactories.add(new FGRegionFactory());
        handlerFactories = new ArrayList<>();
        handlerFactories.add(new FGHandlerFactory());
    }

    public static FGFactoryManager getInstance() {
        return ourInstance;
    }

    public IRegion createRegion(String name, String type, String arguments, CommandSource source) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (isIn(rf.getAliases(), type)) {
                IRegion region = rf.createRegion(name, type, arguments, source);
                if (region != null) return region;
            }
        }
        return null;
    }

    public IRegion createRegion(DataSource source, String name, String type, boolean isEnabled) throws SQLException {
        for (IRegionFactory rf : regionFactories) {
            if (isIn(rf.getTypes(), type)) {
                IRegion region = rf.createRegion(source, name, type, isEnabled);
                if (region != null) return region;
            }
        }
        return null;
    }


    public IHandler createHandler(String name, String type, int priority, String args, CommandSource source) {
        for (IHandlerFactory hf : handlerFactories) {
            if (isIn(hf.getAliases(), type)) {
                IHandler handler = hf.createHandler(name, type, priority, args, source);
                if (handler != null) return handler;
            }
        }
        return null;
    }

    public IHandler createHandler(DataSource source, String name, String type, int priority, boolean isEnabled) throws SQLException {
        for (IHandlerFactory hf : handlerFactories) {
            if (isIn(hf.getTypes(), type)) {
                IHandler handler = hf.createHandler(source, name, type, priority, isEnabled);
                if (handler != null) return handler;
            }
        }
        return null;
    }

    public List<String> regionSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (isIn(rf.getTypes(), type)) {
                return rf.createSuggestions(source, arguments, type);
            }
        }
        return ImmutableList.of();
    }

    public List<String> handlerSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IHandlerFactory hf : handlerFactories) {
            if (isIn(hf.getTypes(), type)) {
                return hf.createSuggestions(source, arguments, type);
            }
        }
        return ImmutableList.of();
    }

    public boolean registerRegionFactory(IRegionFactory factory) {
        if (regionFactories.contains(factory)) return false;
        regionFactories.add(factory);
        return true;
    }

    public boolean registerHandlerFactory(IHandlerFactory factory) {
        if (handlerFactories.contains(factory)) return false;
        handlerFactories.add(factory);
        return true;
    }

    public boolean unregister(IFGFactory factory) {
        if (factory instanceof IRegionFactory)
            return regionFactories.remove(factory);
        else if (factory instanceof IHandlerFactory)
            return handlerFactories.remove(factory);
        return false;
    }

    public List<String> getPrimaryHandlerTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : handlerFactories) {
            aliases.addAll(Arrays.asList(factory.getPrimaryAliases()));
        }
        return aliases;
    }

    public List<String> getPrimaryRegionTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : regionFactories) {
            aliases.addAll(Arrays.asList(factory.getPrimaryAliases()));
        }
        return aliases;
    }
}
