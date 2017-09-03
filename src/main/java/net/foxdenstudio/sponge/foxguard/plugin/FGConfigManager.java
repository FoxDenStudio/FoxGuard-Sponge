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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

public final class FGConfigManager {

    private static FGConfigManager instance;

    private boolean cleanupFiles;
    private boolean saveWorldRegionsInWorldDirectories;
    private boolean saveInWorldDirectory;
    private boolean ownerFirst;
    private boolean useConfigDirectory;
    private boolean useCustomDirectory;
    private Path customDirectory;

    private boolean gcAndFinalize;
    private boolean lockDatabaseFiles;
    private boolean useMMappedFiles;
    private boolean gcCleanerHack;
    private int nameLengthLimit;

    private Map<Module, Boolean> modules = new EnumMap<>(Module.class);

    public FGConfigManager() {
        if (instance == null) instance = this;
        load();
    }

    public static FGConfigManager getInstance() {
        if (instance == null) new FGConfigManager();
        return instance;
    }

    public void save() {
        Path configFile =
                FoxGuardMain.instance().getConfigDirectory().resolve("foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(configFile).build();
        if (Files.exists(configFile)) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        root.getNode("storage", "cleanupFiles")
                .setValue(cleanupFiles)
                .setComment("Sets whether to aggressively delete files that are no longer used. Default: true\n" +
                        "This is meant to keep the file store clean and free of clutter. It also improves load times.\n" +
                        "The caveat is that objects that fail to load are deleted without warning. This normally isn't an issue, even in server crashes.\n" +
                        "However, modifying databases and moving the files around can trigger the cleanup.\n" +
                        "If plugin simply fails to load the database, it would just be discarded.\n" +
                        "Setting this option to false will prevent databases from being deleted.\n" +
                        "However, they will still be overwritten if a new database is made with the same name.");

        root.getNode("storage", "location")
                .setComment("These options control where FoxGuard objects are stored.\n" +
                        "BE WARNED that changing these settings will not automatically move files to a new location.\n" +
                        "YOU MUST do that move yourself. It is advised that you");

        root.getNode("storage", "location", "saveInWorldDirectory")
                .setValue(saveInWorldDirectory)
                .setComment("Whether or not FoxGuard should save object information in the world directory. Default: true\n" +
                        "This includes super-regions, handlers, and controllers, but does not include world-regions.\n" +
                        "If set to false, files will be placed in a directory in the server root directory.");

        root.getNode("storage", "location", "saveWorldRegionsInWorldDirectories")
                .setValue(saveWorldRegionsInWorldDirectories)
                .setComment("Whether or not FoxGuard should save world-region information in the world directory. Default: true\n" +
                        "In this case, the files are kept with their corresponding world/dimension.\n" +
                        "This makes it easier to copy and paste world data without causing de-synchronization between the world data and FoxGuard data.");
        root.getNode("storage", "location", "ownerFirst")
                .setValue(ownerFirst)
                .setComment("Whether to sort by owners first and then category. Default: true\n" +
                        "When set to true, object will be stored like \"foxguard/owners/uuid/handlers/myhandler\".\n" +
                        "When set to false, objects will instead be stored like \"foxguard/handlers/owners/uuid/myhandler\".\n" +
                        "This does not affect objects without an owner, which are still stored like \"foxguard/handlers/myhandler\".");
        root.getNode("storage", "location", "useConfigDirectory")
                .setValue(useConfigDirectory)
                .setComment("Whether or not to place the foxguard directory inside the config directory. Default: false\n" +
                        "Only applies if files are not kept inside the world directory.");
        root.getNode("storage", "location", "useCustomDirectory")
                .setValue(useCustomDirectory)
                .setComment("Whether or not to set the foxguard directory to a custom path. Default false:\n" +
                        "Only applies if files are not kept inside the world folder.\n" +
                        "This setting overrides the other location settings.\n" +
                        "The working directory is the saves directory, which is the root directory on a Minecraft server.");
        root.getNode("storage", "location", "customDirectory")
                .setValue(customDirectory.normalize().toString())
                .setComment("The custom foxguard directory path.");
        root.getNode("storage", "gcAndFinalize")
                .setValue(gcAndFinalize)
                .setComment("Whether to run try running gc and finalization when deleting things.\n" +
                        "This may drastically slow down the deletion of objects.\n" +
                        "Use only if you are having trouble deleting things from in game.\n" +
                        "This really only makes a difference on Windows, so you can leave this alone on Unix based operating systems.");
        root.getNode("storage", "database", "lockDatabaseFiles")
                .setValue(lockDatabaseFiles)
                .setComment("Whether to put a lock on database files while accessing them.\n" +
                        "Locking is known to cause Java to hang on Unix based operating systems running on a NFS (Networked File System) that does not properly support locking.\n" +
                        "This is often the case if you are using a server host, so be very cautious.\n" +
                        "If your server hangs and crashes from the Minecraft watchdog, try setting this to false.");
        root.getNode("storage", "database", "useMMappedFiles")
                .setValue(useMMappedFiles)
                .setComment("Whether to enable memory mapping for database files.\n" +
                        "This has the potential to greatly speed up saving and loading from database files." +
                        "This is known to cause some issues on Windows.\n" +
                        "This may be correctable with gcCleanerHack.");
        root.getNode("storage", "database", "gcCleanerHack")
                .setValue(gcCleanerHack)
                .setComment("Whether to enable MapDB's gcCleanerHack functionality.\n" +
                        "This is meant for fixing issues with databases being un-deletable on Windows when memory mapping is enabled.\n" +
                        "This only makes a difference if memory mapping is enabled, and can potentially decrease performance.");
        root.getNode("general", "nameLengthLimit")
                .setValue(nameLengthLimit)
                .setComment("The length limit for object names. Use 0 or lower for no limit.\n" +
                        "Extremely long names can cause a variety of unfixable issues. You have been warned.");

        for (Module m : Module.values()) {
            root.getNode("module", m.name).setValue(this.modules.get(m));
        }


        //--------------------------------------------------------------------------------------------------------------
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        Path configFile =
                FoxGuardMain.instance().getConfigDirectory().resolve("foxguard.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(configFile).build();
        if (Files.exists(configFile)) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        //--------------------------------------------------------------------------------------------------------------

        cleanupFiles = root.getNode("storage", "cleanupFiles").getBoolean(true);
        saveInWorldDirectory = root.getNode("storage", "location", "saveInWorldDirectory").getBoolean(true);
        saveWorldRegionsInWorldDirectories = root.getNode("storage", "location", "saveWorldRegionsInWorldDirectories").getBoolean(true);
        ownerFirst = root.getNode("storage", "location", "ownerFirst").getBoolean(true);
        useConfigDirectory = root.getNode("storage", "location", "useConfigDirectory").getBoolean(false);
        useCustomDirectory = root.getNode("storage", "location", "useCustomDirectory").getBoolean(false);
        customDirectory = root.getNode("storage", "location", "customDirectory").getValue(o -> {
            Path path = null;
            if (o instanceof Path) path = (Path) o;
            else if (o instanceof String) path = Paths.get((String) o);
            if (path == null) return null;
            if (Files.notExists(path) || Files.isDirectory(path)) return path;
            else return null;
        }, Paths.get("foxguard"));
        gcAndFinalize = root.getNode("storage", "gcAndFinalize").getBoolean(false);
        lockDatabaseFiles = root.getNode("storage", "database", "lockDatabaseFiles").getBoolean(false);
        useMMappedFiles = root.getNode("storage", "database", "useMMappedFiles").getBoolean(false);
        gcCleanerHack = root.getNode("storage", "database", "gcCleanerHack").getBoolean(false);
        nameLengthLimit = root.getNode("general", "nameLengthLimit").getInt(24);
        for (Module m : Module.values()) {
            this.modules.put(m, root.getNode("module", m.name).getBoolean(true));
        }

        //--------------------------------------------------------------------------------------------------------------

        //Path path = Sponge.getGame().getSavesDirectory();
    }


    public boolean cleanupFiles() {
        return cleanupFiles;
    }

    public boolean saveWorldRegionsInWorldFolders() {
        return saveWorldRegionsInWorldDirectories;
    }

    public boolean ownerFirst() {
        return ownerFirst;
    }

    public boolean saveInWorldFolder() {
        return saveInWorldDirectory;
    }

    public boolean useConfigFolder() {
        return useConfigDirectory;
    }

    public boolean gcAndFinalize() {
        return gcAndFinalize;
    }

    public boolean lockDatabaseFiles() {
        return lockDatabaseFiles;
    }

    public boolean useMMappedFiles() {
        return useMMappedFiles;
    }

    public boolean gcCleanerHack() {
        return gcCleanerHack;
    }

    public int getNameLengthLimit() {
        return nameLengthLimit;
    }

    public Map<Module, Boolean> getModules() {
        return this.modules;
    }

    public enum Module {
        MOVEMENT("movement");

        String name;

        Module(String name) {
            this.name = name;
        }
    }

}
