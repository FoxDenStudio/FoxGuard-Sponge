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
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.HandlerData;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectData;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public final class FGFactoryManager {

    private static final FGFactoryManager ourInstance = new FGFactoryManager();
    private final List<IRegionFactory> regionFactories;
    private final List<IWorldRegionFactory> worldRegionFactories;
    private final List<IHandlerFactory> handlerFactories;
    private final List<IControllerFactory> controllerFactories;

    private FGFactoryManager() {
        regionFactories = new ArrayList<>();
        worldRegionFactories = new ArrayList<>();
        handlerFactories = new ArrayList<>();
        controllerFactories = new ArrayList<>();
    }

    public static FGFactoryManager getInstance() {
        return ourInstance;
    }

    public IRegion createRegion(String name, String type, String arguments, CommandSource source) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (isIn(rf.getAliases(), type)) {
                IRegion region = rf.create(name, arguments, source);
                if (region != null) return region;
            }
        }
        return null;
    }

    public IRegion createRegion(Path directory, String type, FGObjectData data) {
        for (IRegionFactory rf : regionFactories) {
            if (rf.getType().equalsIgnoreCase(type)) {
                IRegion region = rf.create(directory, data);
                if (region != null) return region;
            }
        }
        return null;
    }

    public IWorldRegion createWorldRegion(String name, String type, String arguments, CommandSource source) throws CommandException {
        for (IWorldRegionFactory wrf : worldRegionFactories) {
            if (isIn(wrf.getAliases(), type)) {
                IWorldRegion region = wrf.create(name, arguments, source);
                if (region != null) return region;
            }
        }
        return null;
    }

    public IWorldRegion createWorldRegion(Path directory, String type, FGObjectData data) {
        for (IWorldRegionFactory wrf : worldRegionFactories) {
            if (wrf.getType().equalsIgnoreCase(type)) {
                IWorldRegion region = wrf.create(directory, data);
                if (region != null) return region;
            }
        }
        return null;
    }


    public IHandler createHandler(String name, String type, int priority, String args, CommandSource source) throws CommandException {
        for (IHandlerFactory hf : handlerFactories) {
            if (isIn(hf.getAliases(), type)) {
                IHandler handler = hf.create(name, args, source);
                if (handler != null) return handler;
            }
        }
        return null;
    }

    public IHandler createHandler(Path directory, String type, HandlerData data) {
        for (IHandlerFactory hf : handlerFactories) {
            if (hf.getType().equalsIgnoreCase(type)) {
                IHandler handler = hf.create(directory, data);
                if (handler != null) return handler;
            }
        }
        return null;
    }

    public IController createController(String name, String type, int priority, String args, CommandSource source) throws CommandException {
        for (IControllerFactory cf : controllerFactories) {
            if (isIn(cf.getAliases(), type)) {
                IController controller = cf.create(name, args, source);
                if (controller != null) return controller;
            }
        }
        return null;
    }

    public IController createController(Path directory, String type, HandlerData data) {
        for (IControllerFactory cf : controllerFactories) {
            if (cf.getType().equalsIgnoreCase(type)) {
                IController controller = cf.create(directory, data);
                if (controller != null) return controller;
            }
        }
        return null;
    }

    public List<String> regionSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (rf.getType().equalsIgnoreCase(type)) {
                return rf.createSuggestions(source, arguments, type, null);
            }
        }
        return ImmutableList.of();
    }

    public List<String> worldRegionSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IWorldRegionFactory wrf : worldRegionFactories) {
            if (wrf.getType().equalsIgnoreCase(type)) {
                return wrf.createSuggestions(source, arguments, type, null);
            }
        }
        return ImmutableList.of();
    }

    public List<String> handlerSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IHandlerFactory hf : handlerFactories) {
            if (hf.getType().equalsIgnoreCase(type)) {
                return hf.createSuggestions(source, arguments, type, null);
            }
        }
        return ImmutableList.of();
    }

    public List<String> controllerSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        for (IControllerFactory cf : controllerFactories) {
            if (cf.getType().equalsIgnoreCase(type)) {
                return cf.createSuggestions(source, arguments, type, null);
            }
        }
        return ImmutableList.of();
    }

    public boolean registerRegionFactory(IRegionFactory factory) {
        if (regionFactories.contains(factory)) return false;
        regionFactories.add(factory);
        return true;
    }

    public boolean registerWorldRegionFactory(IWorldRegionFactory factory) {
        if (worldRegionFactories.contains(factory)) return false;
        worldRegionFactories.add(factory);
        return true;
    }

    public boolean registerHandlerFactory(IHandlerFactory factory) {
        if (handlerFactories.contains(factory)) return false;
        handlerFactories.add(factory);
        return true;
    }

    public boolean registerControllerFactory(IControllerFactory factory) {
        if (controllerFactories.contains(factory)) return false;
        controllerFactories.add(factory);
        return true;
    }

    public boolean unregister(IFGFactory factory) {
        if (factory instanceof IRegionFactory) {
            if (factory instanceof IWorldRegionFactory)
                return worldRegionFactories.remove(factory);
            else return regionFactories.remove(factory);
        } else if (factory instanceof IHandlerFactory) {
            if (factory instanceof IControllerFactory)
                return controllerFactories.remove(factory);
            else return handlerFactories.remove(factory);
        }
        return false;
    }

    public List<String> getPrimaryRegionTypeAliases() {
        return regionFactories.stream().map(IFGFactory::getPrimaryAlias).collect(Collectors.toList());
    }

    public List<String> getPrimaryWorldRegionTypeAliases() {
        return worldRegionFactories.stream().map(IFGFactory::getPrimaryAlias).collect(Collectors.toList());
    }

    public List<String> getPrimaryHandlerTypeAliases() {
        return handlerFactories.stream().map(IFGFactory::getPrimaryAlias).collect(Collectors.toList());
    }

    public List<String> getPrimaryControllerTypeAliases() {
        return controllerFactories.stream().map(IFGFactory::getPrimaryAlias).collect(Collectors.toList());
    }

    public List<String> getRegionTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : regionFactories) {
            aliases.addAll(Arrays.asList(factory.getAliases()));
        }
        return aliases;
    }

    public List<String> getWorldRegionTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : worldRegionFactories) {
            aliases.addAll(Arrays.asList(factory.getAliases()));
        }
        return aliases;
    }

    public List<String> getHandlerTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : handlerFactories) {
            aliases.addAll(Arrays.asList(factory.getAliases()));
        }
        return aliases;
    }

    public List<String> getControllerTypeAliases() {
        List<String> aliases = new ArrayList<>();
        for (IFGFactory factory : controllerFactories) {
            aliases.addAll(Arrays.asList(factory.getAliases()));
        }
        return aliases;
    }
}
