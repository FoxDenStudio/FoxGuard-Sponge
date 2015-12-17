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

import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;

public final class FGConfigManager {

    private static FGConfigManager instance;


    private boolean forceLoad;
    private boolean purgeDatabases;
    private boolean threadSafe = false;


    public FGConfigManager() {
        if (instance == null) instance = this;
        load();
    }

    public static FGConfigManager getInstance() {
        if (instance == null) new FGConfigManager();
        return instance;
    }

    private void load() {
        File configFile = new File(
                FoxGuardMain.instance().getConfigDirectory().getPath() + "/foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setFile(configFile).build();
        if (configFile.exists()) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        forceLoad = root.getNode("storage", "forceLoad").getBoolean(false);
        purgeDatabases = root.getNode("storage", "purgeDatabases").getBoolean(true);
        threadSafe = root.getNode("threading", "threadSafe").getBoolean(false);

        //--------------------------------------------------------------------------------------------------------------
    }

    public void save() {
        File configFile = new File(
                FoxGuardMain.instance().getConfigDirectory().getPath() + "/foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setFile(configFile).build();
        if (configFile.exists()) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        root.getNode("storage", "forceLoad").setComment("Enables force loading of Region and Handler databases. Default: false\n" +
                "This allows loading of Regions and Handlers whose metadata don't match saved records.\n" +
                "This usually occurs when a database file is replaced with another of the same name, but the internal metadata doesn't match.\n" +
                "FoxGuard will attempt to resolve these errors, however there is no guarantee that it will work or even behave at all.\n" +
                "MAY CAUSE UNPREDICTABLE RESULTS! USE WITH CAUTION!!! It is recommended to use the \"import\" feature instead.")
                .setValue(forceLoad);

        root.getNode("storage", "purgeDatabases").setComment("Sets whether to aggressively delete databases that appear corrupted or are no longer used. Default: true\n" +
                "This is meant to keep the database store clean and free of clutter. It also improves load times.\n" +
                "The caveat is that corrupted databases are deleted without warning. This normally isn't an issue, even in server crashes.\n" +
                "However, modifying databases and moving the files around can trigger the cleanup.\n" +
                "If force loading is off or the plugin simply fails to load the database, it would just be discarded.\n" +
                "Setting this option to false will prevent databases from being deleted.\n" +
                "However, they will still be overwritten if a new database is made with the same name.")
                .setValue(purgeDatabases);
        root.getNode("threading", "threadSafe").setComment("Set this to true if you are using a multi-threaded server like glowstone.\n" +
                "You can also try enabling this if you are seeing ConcurrentModificationException errors in the logs.\n" +
                "However be careful, as enabling this on single threaded servers like SpongeForge and SpongeVanilla will significantly decrease performance.")
                .setValue(threadSafe);

        //--------------------------------------------------------------------------------------------------------------
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean forceLoad() {
        return forceLoad;
    }

    public boolean purgeDatabases() {
        return purgeDatabases;
    }

    public boolean threadSafe() {
        return threadSafe;
    }
}
