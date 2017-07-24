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

package net.foxdenstudio.sponge.foxguard.plugin.storage;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.plugin.util.IWorldBound;
import net.foxdenstudio.sponge.foxguard.plugin.FGConfigManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxguard.plugin.FGManager.SERVER_UUID;

/**
 * Created by Fox on 7/8/2017.
 * Project: SpongeForge
 */
public class FGStorageManagerNew {

    public static final String[] FS_ILLEGAL_NAMES = {"con", "prn", "aux", "nul",
            "com0", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt0", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"};
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final Gson GSON = new Gson();
    private static FGStorageManagerNew instance;
    public final HashMap<IFGObject, Boolean> defaultModifiedMap;
    private final UserStorageService userStorageService;
    private final Logger logger = FoxGuardMain.instance().getLogger();
    private final FGManager manager = FGManager.getInstance();
    private final Path foxguardDirectory = getFoxguardDirectory();
    private final Map<World, Path> worldDirectories;

    private boolean handlersLoaded = false;

    private FGStorageManagerNew() {
        userStorageService = FoxGuardMain.instance().getUserStorage();
        defaultModifiedMap = new CacheMap<>((k, m) -> {
            if (k instanceof IFGObject) {
                m.put((IFGObject) k, true);
                return true;
            } else return null;
        });
        worldDirectories = new CacheMap<>((k, m) -> {
            if (k instanceof World) {
                Path dir = getWorldDirectory((World) k);
                m.put((World) k, dir);
                return dir;
            } else return null;
        });
    }

    public static FGStorageManagerNew getInstance() {
        if (instance == null) instance = new FGStorageManagerNew();
        return instance;
    }

    public void saveHandlerIndex() {
        logger.info("Saving handler index");
        Path file = foxguardDirectory.resolve(FGTypes.HANDLER.getIndexFile());
        Set<IHandler> handlers = manager.getHandlers();
        saveIndex(handlers, file);
    }

    public void saveRegionIndex() {
        logger.info("Saving region index");
        Path file = foxguardDirectory.resolve(FGTypes.REGION.getIndexFile());
        Set<IRegion> regions = manager.getRegions();
        saveIndex(regions, file);
    }

    public void saveWorldRegionIndex(World world) {
        logger.info("Saving worldregion index");
        Path file = worldDirectories.get(world).resolve(FGTypes.WORLDREGION.getIndexFile());
        Set<IWorldRegion> worldRegions = manager.getWorldRegions(world);
        saveIndex(worldRegions, file);
    }

    private void saveIndex(Set<? extends IFGObject> objects, Path file) {
        List<FGObjectIndex> indexList = objects.stream().map(FGObjectIndex::new).collect(Collectors.toList());
        try (JsonWriter jsonWriter = new JsonWriter(Files.newBufferedWriter(file, CHARSET))) {
            jsonWriter.setIndent("    ");
            GSON.toJson(indexList, List.class, jsonWriter);
        } catch (IOException e) {
            logger.error("Failed to open index for writing: " + file, e);
        }
    }

    public void saveObjects(Set<? extends IFGObject> objects, boolean force) {
        objects.forEach(object -> {
            String name = object.getName();
            UUID owner = object.getOwner();
            boolean isOwned = !owner.equals(SERVER_UUID);
            Optional<User> userOwner = userStorageService.get(owner);
            String logName = (userOwner.map(user -> user.getName() + ":").orElse("")) + (isOwned ? owner + ":" : "") + name;
            if (object.autoSave()) {
                Path directory = getObjectDirectory(object);

            } else {
                logger.info("Region " + logName + " does not need saving. Skipping...");
            }
        });
    }


    public void constructDirectory(Path directory) {
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) return;
            else {
                logger.warn("There is a file at " + directory + " where a directory was expected. Deleting and replacing with a directory...");
                try {
                    Files.delete(directory);
                } catch (IOException e) {
                    logger.error("Error deleting the file: " + directory, e);
                    return;
                }
            }
        }

        try {
            int counter = 1;
            float time = 0.25f;
            while (true) {
                try {
                    Files.createDirectories(directory);
                    break;
                } catch (AccessDeniedException e) {
                    if (counter > 5) throw e;
                    else {
                        logger.error("Unable to create directory: " + directory + "  Trying again in " + time + " second(s)", e);
                        try {
                            Thread.sleep((long) (1000 * time));
                        } catch (InterruptedException e1) {
                            logger.warn("Thread sleep was interrupted: ", e);
                        }
                    }
                }
                counter++;
                time *= 2;
            }
            logger.info("Created directory: " + directory);
        } catch (IOException e) {
            logger.error("There was an error creating the directory: " + directory, e);
        }
    }

    private Path getFoxguardDirectory() {
        Path path = Sponge.getGame().getSavesDirectory();
        if (FGConfigManager.getInstance().saveInWorldFolder()) {
            path = path.resolve(Sponge.getServer().getDefaultWorldName());
        } else if (FGConfigManager.getInstance().useConfigFolder()) {
            path = FoxGuardMain.instance().getConfigDirectory();
        }
        path = path.resolve("foxguard");
        constructDirectory(path);
        return path;
    }

    private Path getWorldDirectory(World world) {
        Path path = Sponge.getGame().getSavesDirectory();
        if (FGConfigManager.getInstance().saveWorldRegionsInWorldFolders()) {
            path = world.getDirectory();
            path = path.resolve("foxguard");
        } else {
            if (FGConfigManager.getInstance().useConfigFolder()) {
                path = FoxGuardMain.instance().getConfigDirectory();
            }
            path = path.resolve("foxguard").resolve("worlds").resolve(world.getName());
        }
        constructDirectory(path);
        return path;
    }

    private void deleteDirectory(Path directory) {
        deleteDirectory(directory, false);
    }

    private void deleteDirectory(Path directory, boolean innerOnly) {
        FoxGuardMain.instance().getLogger().info("Deleting directory: " + directory);
        if (Files.exists(directory) && Files.isDirectory(directory))
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            Files.delete(file);
                            logger.info("Deleted file: " + file);
                        } catch (IOException e) {
                            logger.error("There was an error deleting the file: " + file, e);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc == null && !(innerOnly && Files.isSameFile(dir, directory))) {
                            try {
                                Files.delete(dir);
                                logger.info("Deleted directory: " + dir);
                            } catch (IOException e) {
                                logger.error("There was an error deleting the directory: " + dir, e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("There was an error while trying to recursively delete the directory: " + directory, e);
            }
        else if (Files.exists(directory)) {
            logger.warn(directory + "is a file. A directory was expected. Deleting...");
            try {
                Files.delete(directory);
            } catch (IOException e) {
                logger.error("There was an error deleting the file: " + directory, e);
            }
        }
    }

    private boolean isEmptyDirectory(Path directory) {
        if (Files.notExists(directory)) return true;
        if (!Files.isDirectory(directory)) return false;
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(directory);
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            logger.error("Could not read contents of directory: " + directory, e);
            return false;
        }
    }

    private Path getObjectDirectory(IFGObject object) {
        Path dir;
        if(object instanceof IWorldBound){
            dir = worldDirectories.get(((IWorldBound) object).getWorld());
        } else {
            dir = foxguardDirectory;
        }
        FGTypes type = getObjectType(object);
        dir.resolve(type.fileName);

        UUID owner = object.getOwner();
        if (!owner.equals(SERVER_UUID)) {
            dir.resolve("users").resolve(owner.toString());
        }
        dir.resolve(object.getName().toLowerCase());
        constructDirectory(dir);
        return dir;
    }

    private FGTypes getObjectType(IFGObject object){
        if(object instanceof IRegion){
            if(object instanceof IWorldRegion){
                return FGTypes.WORLDREGION;
            }
            return FGTypes.REGION;
        } else if (object instanceof IHandler){
            if(object instanceof IController){
                return FGTypes.CONTROLLER;
            }
            return FGTypes.HANDLER;
        }
        return FGTypes.OBJECT;
    }

    private enum FGTypes {
        REGION("Region", "regions"),
        WORLDREGION("World region", "wregions"),
        HANDLER("Handler", "handlers"),
        CONTROLLER("Controller", "handlers"),
        OBJECT("Object", "objects");

        String nameUppercase;
        String fileName;

        FGTypes(String nameUppercase, String directoryName) {
            this.nameUppercase = nameUppercase;
            this.fileName = directoryName;
        }

        public String getIndexFile(){
            return fileName + ".foxcf";
        }
    }
}
