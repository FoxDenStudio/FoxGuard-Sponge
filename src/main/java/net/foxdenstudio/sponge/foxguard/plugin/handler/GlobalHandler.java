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

package net.foxdenstudio.sponge.foxguard.plugin.handler;

import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Entry;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.nio.file.Path;
import java.util.*;

public class GlobalHandler extends StaticHandler implements IGlobal {

    public static final String NAME = "_global";

    public GlobalHandler() {
        super(NAME, true, Integer.MIN_VALUE / 2);
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Integer.MIN_VALUE / 2;
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public String getShortTypeName() {
        return "Global";
    }

    @Override
    public String getLongTypeName() {
        return "Global";
    }

    @Override
    public String getUniqueTypeString() {
        return "global";
    }


    public void load(Path directory) {
        Path flagsFile = directory.resolve("flags.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(flagsFile).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(flagsFile, loader);
        List<Optional<String>> optionalFlagsList = root.getList(o -> {
            if (o instanceof String) {
                return Optional.of((String) o);
            } else return Optional.empty();
        });
        this.entries.clear();
        optionalFlagsList.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Entry::deserialize)
                .forEach(this.entries::add);
        this.permCache.clear();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = true;
    }

}
