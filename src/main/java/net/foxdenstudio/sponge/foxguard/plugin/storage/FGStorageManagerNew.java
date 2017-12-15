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

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.util.IWorldBound;
import net.foxdenstudio.sponge.foxguard.plugin.FGConfigManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.HandlerData;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.FGObjectData;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
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
    public static final String METADATA_FILE_NAME = "metadata.foxcf";
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Type INDEX_LIST_TYPE = new TypeToken<List<FGSObjectIndex>>() {
    }.getType();
    private static FGStorageManagerNew instance;
    public final HashMap<IFGObject, Boolean> defaultModifiedMap;
    private final FGConfigManager config;
    private final UserStorageService userStorageService;
    private final Logger logger = FoxGuardMain.instance().getLogger();
    private final FGManager manager = FGManager.getInstance();
    private final Map<World, Path> worldDirectories;
    private Gson gson;
    private boolean prettyPrint;
    private String gsonIndentString;
    private Path fgDirectory;
    private boolean serverLoaded = false;
    private boolean reentry = false;

    private FGStorageManagerNew() {
        config = FGConfigManager.getInstance();
        userStorageService = FoxGuardMain.instance().getUserStorage();
        defaultModifiedMap = new CacheMap<>((k, m) -> {
            if (k instanceof IFGObject) {
                m.put((IFGObject) k, true);
                return true;
            } else return null;
        });
        worldDirectories = new HashMap<>();
        prettyPrint = config.prettyPrint();
        StringBuilder builder = new StringBuilder();
        int indent = config.prettyPrintIndent();
        for (int i = 0; i < indent; i++) {
            builder.append(" ");
        }
        gsonIndentString = builder.toString();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();

    }

    public static FGStorageManagerNew getInstance() {
        if (instance == null) instance = new FGStorageManagerNew();
        return instance;
    }

    public void saveHandlerIndex() {
        logger.info("Saving handler index");
        Path file = getFGDirectory().resolve(FGCat.HANDLER.getIndexFile());
        Set<IHandler> handlers = manager.getHandlers();
        saveIndex(handlers, file);
    }

    public void saveRegionIndex() {
        logger.info("Saving region index");
        Path file = getFGDirectory().resolve(FGCat.REGION.getIndexFile());
        Set<IRegion> regions = manager.getRegions();
        saveIndex(regions, file);
    }

    public void saveWorldRegionIndex(World world) {
        logger.info("Saving worldregion index for world: " + world.getName());
        Path file = getWorldDirectory(world).resolve(FGCat.WORLDREGION.getIndexFile());
        Set<IWorldRegion> worldRegions = manager.getWorldRegions(world);
        saveIndex(worldRegions, file);
    }

    private void saveIndex(Set<? extends IFGObject> objects, Path file) {
        List<FGSObjectIndex> indexList = new ArrayList<>();

        objects.stream().sorted().forEach(object -> {
            boolean saveLinks = (object instanceof ILinkable && ((ILinkable) object).saveLinks());
            boolean autoSave = object.autoSave();
            if (autoSave || saveLinks) {
                FGSObjectIndex index = new FGSObjectIndex(object);
                if (!autoSave) {
                    index.type = null;
                    index.enabled = null;
                    index.priority = null;
                }
                indexList.add(index);
            }
        });

        try (JsonWriter jsonWriter = getJsonWriter(Files.newBufferedWriter(file, CHARSET))) {
            gson.toJson(indexList, List.class, jsonWriter);
        } catch (IOException e) {
            logger.error("Failed to open index for writing: " + file, e);
        }
    }

    private void updateIndexFor(IFGObject object) {
        if (object instanceof IHandler) {
            saveHandlerIndex();
        } else if (object instanceof IRegion) {
            if (object instanceof IWorldRegion) {
                saveWorldRegionIndex(((IWorldRegion) object).getWorld());
            } else {
                saveRegionIndex();
            }
        }
    }

    public void saveObject(IFGObject object) {
        saveObject(object, false);
    }

    public void saveObject(IFGObject object, boolean force) {
        if (reentry) return;
        FGCat type = getObjectType(object);
        String logName = FGUtil.getLogName(object);

        //System.out.println(name +",  " + owner + ",  " + isOwned + ",  " + userOwner + ",  " + logName);

        if (object.autoSave()) {

            boolean shouldSave = object.shouldSave();
            if (force || shouldSave) {
                Path directory = getObjectDirectory(object);
                String category = FGUtil.getCategory(object);
                logger.info((shouldSave ? "S" : "Force s") + "aving " + category + " " + logName + " in directory: " + directory);
                try {
                    object.save(directory);
                } catch (Exception e) {
                    logger.error("There was an error while saving " + FGUtil.getCategory(object) + " " + logName + "!", e);
                }

                logger.info("Saving metadata for " + category + " " + logName);
                FGSObjectMeta metadata = new FGSObjectMeta(object);
                Path metadataFile = directory.resolve(METADATA_FILE_NAME);
                try (JsonWriter jsonWriter = getJsonWriter(Files.newBufferedWriter(metadataFile, CHARSET))) {
                    gson.toJson(metadata, FGSObjectMeta.class, jsonWriter);
                } catch (IOException e) {
                    logger.error("Failed to open metadata for writing: " + metadataFile, e);
                }
            } else {
                logger.info(type.uName + " " + logName + " is already up to date. Skipping...");
            }

            defaultModifiedMap.put(object, false);
        } else {
            logger.info(type.uName + " " + logName + " does not need saving. Skipping...");
        }
    }

    public void saveObjects(Set<? extends IFGObject> objects) {
        saveObjects(objects, false);
    }

    public void saveObjects(Set<? extends IFGObject> objects, boolean force) {
        if (reentry) return;
        objects.forEach(object -> saveObject(object, force));
    }

    public void addObject(IFGObject object) {
        if (reentry) return;
        Path directory = getObjectDirectory(object);
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) {
                deleteDirectory(directory, true);
            } else {
                logger.warn("Found file instead of directory. Deleting.");
                try {
                    Files.delete(directory);
                } catch (IOException e) {
                    logger.error("Unable to delete file: " + directory, e);
                    return;
                }
            }
            saveObject(object);
            updateIndexFor(object);

        }
    }

    public void removeObject(IFGObject object) {
        if (reentry) return;
        Path directory = getObjectDirectory(object);
        if (config.cleanupFiles()) {
            if (Files.exists(directory)) {
                if (Files.isDirectory(directory)) {
                    deleteDirectory(directory);
                } else {
                    try {
                        Files.delete(directory);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void loadServer() {
        if (serverLoaded) return;
        boolean oldReentry = reentry;
        reentry = true;

        FGManager manager = FGManager.getInstance();
        FGSLegacyLoader legacyLoader = FGSLegacyLoader.getInstance();

        logger.info("Loading regions");
        Path regionIndexFile = getFGDirectory().resolve(FGCat.REGION.getIndexFile());
        Optional<List<FGSObjectIndex>> regionIndexOpt = loadIndex(regionIndexFile);
        if (!regionIndexOpt.isPresent()) {
            regionIndexFile = getFGDirectory().resolve(legacyLoader.getIndexDBName(FGCat.REGION.pathName));
            regionIndexOpt = legacyLoader.getLegacyIndex(regionIndexFile);
        }
        class RegionEntry {
            IRegion region;
            FGSObjectIndex index;

            public RegionEntry(IRegion region, FGSObjectIndex index) {
                this.region = region;
                this.index = index;
            }
        }
        List<RegionEntry> loadedRegions = new ArrayList<>();
        List<FGSObjectIndex> extraRegionLinks = new ArrayList<>();
        if (regionIndexOpt.isPresent()) {
            List<FGSObjectIndex> regionIndex = regionIndexOpt.get();
            for (FGSObjectIndex index : regionIndex) {
                FGCat fgCat = FGCat.from(index.category);
                if (fgCat == FGCat.REGION) {
                    Path directory = getObjectDirectory(index, null);
                    Optional<IFGObject> fgObjectOptional = loadObject(directory, index, null);
                    if (fgObjectOptional.isPresent()) {
                        IFGObject fgObject = fgObjectOptional.get();
                        if (fgObject instanceof IRegion) {
                            IRegion region = (IRegion) fgObject;
                            manager.addRegion(region);
                            loadedRegions.add(new RegionEntry(region, index));
                        }
                    } else if (index.links != null && !index.links.isEmpty()) {
                        extraRegionLinks.add(index);
                    }
                } else {
                    logger.warn("Found an entry of incorrect category. Expected region, found: " + index.category);
                }
            }
        }

        logger.info("Loading handlers");
        Path handlerIndexFile = getFGDirectory().resolve(FGCat.HANDLER.getIndexFile());
        Optional<List<FGSObjectIndex>> handlerIndexOpt = loadIndex(handlerIndexFile);
        if (!handlerIndexOpt.isPresent()) {
            handlerIndexFile = getFGDirectory().resolve(legacyLoader.getIndexDBName(FGCat.HANDLER.pathName));
            handlerIndexOpt = legacyLoader.getLegacyIndex(handlerIndexFile);
        }
        class ControllerEntry {
            IController controller;
            FGSObjectIndex index;
            Path directory;

            public ControllerEntry(IController controller, FGSObjectIndex index, Path directory) {
                this.controller = controller;
                this.index = index;
                this.directory = directory;
            }
        }
        List<ControllerEntry> loadedControllers = new ArrayList<>();
        if (handlerIndexOpt.isPresent()) {
            List<FGSObjectIndex> handlerIndex = handlerIndexOpt.get();
            for (FGSObjectIndex index : handlerIndex) {
                FGCat fgCat = FGCat.from(index.category);
                if (fgCat == FGCat.HANDLER || fgCat == FGCat.CONTROLLER) {
                    Path directory = getObjectDirectory(index, null);
                    Optional<IFGObject> fgObjectOptional = loadObject(directory, index, null);
                    if (fgObjectOptional.isPresent()) {
                        IFGObject fgObject = fgObjectOptional.get();
                        if (fgObject instanceof IHandler) {
                            IHandler handler = (IHandler) fgObject;
                            manager.addHandler(handler);
                            if (handler instanceof IController) {
                                loadedControllers.add(new ControllerEntry(((IController) handler), index, directory));
                            }
                        }
                    }
                } else {
                    logger.warn("Found an entry of incorrect category. Expected handler or controller, found: " + index.category);
                }
            }
        }

        logger.info("Loading global handler");
        manager.getGlobalHandler().load(
                getFGDirectory()
                        .resolve(FGCat.HANDLER.pathName)
                        .resolve(GlobalHandler.NAME.toLowerCase())
        );

        logger.info("Loading region links");
        for (RegionEntry entry : loadedRegions) {
            if (entry.index.links != null) {
                Set<IHandler> handlers = entry.index.links.stream()
                        .map(path -> manager.getHandler(path.name, path.owner))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
                handlers.forEach(entry.region::addLink);
                StringBuilder logMessage = new StringBuilder("Linked region ");
                logMessage.append(FGUtil.getLogName(entry.region));
                logMessage.append(" to handlers: ");
                for (Iterator<IHandler> handlerIterator = handlers.iterator(); handlerIterator.hasNext(); ) {
                    IHandler handler = handlerIterator.next();
                    logMessage.append(FGUtil.getLogName(handler));
                    if (handlerIterator.hasNext()) {
                        logMessage.append(", ");
                    }
                }

                logger.info(logMessage.toString());
            }
        }

        logger.info("Loading controller links");
        for (ControllerEntry entry : loadedControllers) {
            List<IHandler> handlers;
            if (entry.index.links != null) {
                handlers = entry.index.links.stream()
                        .map(path -> manager.getHandler(path.name, path.owner))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(GuavaCollectors.toImmutableList());
            } else handlers = ImmutableList.of();

            entry.controller.loadLinks(entry.directory, handlers);
        }


        reentry = oldReentry;
        serverLoaded = true;
    }

    public void loadWorld(World world) {
        boolean oldReentry = reentry;
        reentry = true;

        FGManager manager = FGManager.getInstance();
        FGSLegacyLoader legacyLoader = FGSLegacyLoader.getInstance();

        logger.info("Loading worldregions for world: " + world.getName());
        Path regionIndexFile = getWorldDirectory(world).resolve(FGCat.WORLDREGION.getIndexFile());
        Optional<List<FGSObjectIndex>> worldRegionIndexOpt = loadIndex(regionIndexFile);
        if (!worldRegionIndexOpt.isPresent()) {
            regionIndexFile = getWorldDirectory(world).resolve(legacyLoader.getIndexDBName(FGCat.WORLDREGION.pathName));
            worldRegionIndexOpt = legacyLoader.getLegacyIndex(regionIndexFile);
        }
        if (worldRegionIndexOpt.isPresent()) {
            List<FGSObjectIndex> worldRegionIndex = worldRegionIndexOpt.get();
            for (FGSObjectIndex index : worldRegionIndex) {
                FGCat fgCat = FGCat.from(index.category);
                if (fgCat == FGCat.WORLDREGION) {
                    Path directory = getObjectDirectory(index, world);
                    Optional<IFGObject> fgObjectOptional = loadObject(directory, index, world);
                    if (fgObjectOptional.isPresent()) {
                        IFGObject fgObject = fgObjectOptional.get();
                        if (fgObject instanceof IWorldRegion) {
                            IWorldRegion worldRegion = (IWorldRegion) fgObject;
                            manager.addWorldRegion(worldRegion, world);
                            if (index.links != null) {
                                Set<IHandler> handlers = index.links.stream()
                                        .map(path -> manager.getHandler(path.name, path.owner))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .collect(Collectors.toSet());
                                handlers.forEach(worldRegion::addLink);
                                StringBuilder logMessage = new StringBuilder("Linked worldregion ");
                                logMessage.append(FGUtil.getLogName(worldRegion));
                                logMessage.append(" to handlers: ");
                                for (Iterator<IHandler> handlerIterator = handlers.iterator(); handlerIterator.hasNext(); ) {
                                    IHandler handler = handlerIterator.next();
                                    logMessage.append(FGUtil.getLogName(handler));
                                    if (handlerIterator.hasNext()) {
                                        logMessage.append(", ");
                                    }
                                }

                                logger.info(logMessage.toString());
                            }
                        }
                    }
                } else {
                    logger.warn("Found an entry of incorrect category. Expected worldregion, found: " + index.category);
                }
            }
        }

        reentry = oldReentry;
    }

    public Optional<List<FGSObjectIndex>> loadIndex(Path indexFile) {
        List<FGSObjectIndex> index = null;
        if (Files.exists(indexFile) && !Files.isDirectory(indexFile)) {
            try (JsonReader jsonReader = new JsonReader(Files.newBufferedReader(indexFile))) {
                index = gson.fromJson(jsonReader, INDEX_LIST_TYPE);
            } catch (IOException e) {
                logger.error("Failed to open index for reading: " + indexFile, e);
            }
        } else {
            logger.warn("Index file does not exist: " + indexFile);
        }
        return Optional.ofNullable(index);
    }

    public Optional<IFGObject> loadObject(Path directory, @Nullable FGSObjectIndex index, @Nullable World world) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) return Optional.empty();

        FGSObjectMeta metadata = null;
        Path metadataFile = directory.resolve(METADATA_FILE_NAME);
        if (Files.exists(metadataFile) && !Files.isDirectory(metadataFile)) {
            try (JsonReader jsonReader = new JsonReader(Files.newBufferedReader(metadataFile))) {
                metadata = gson.fromJson(jsonReader, FGSObjectMeta.class);
            } catch (IOException e) {
                logger.error("Failed to open metadata for reading: " + metadataFile, e);
            }
        }

        String name = null;
        String category = null;
        String type = null;
        if (index != null) {
            name = index.name;
            category = index.category;
            type = index.type;
        }
        if (metadata != null) {
            if (name == null || name.isEmpty()) name = metadata.name;
            if (category == null || category.isEmpty()) category = metadata.category;
            if (type == null || type.isEmpty()) type = metadata.type;
        }

        if (category == null || category.isEmpty() || type == null || type.isEmpty()) return Optional.empty();


        FGCat fgCat = FGCat.from(category);
        if (fgCat == null || fgCat == FGCat.OBJECT) return Optional.empty();

        if (isGlobal(fgCat, type, name)) {
            logger.info("Found global " + fgCat.lName + ". Skipping...");
            return Optional.empty();
        }

        UUID owner = null;
        if (index != null) owner = index.owner;
        if (owner == null) owner = SERVER_UUID;

        boolean isOwned = !owner.equals(SERVER_UUID);
        Optional<User> userOwner = isOwned ? userStorageService.get(owner) : Optional.empty();
        UUID finalOwner = owner;
        String ownerName = (userOwner.map(user -> user.getName() + ":" + finalOwner).orElse("None"));


        if (name == null || name.isEmpty()) {
            name = category + "-" + type;
            logger.warn("No name for loaded " + fgCat.lName + ". Using generated name: " + name);
        }
        if (!fgCat.isNameAvailable(name, owner, world)) {
            String oldName = name;
            String nameBase = name + "-";
            int id = 1;
            do {
                name = nameBase + id;
                id++;
            } while (!fgCat.isNameAvailable(name, owner, world));
            logger.warn("Name " + oldName + " for " + fgCat.lName + " already in use. Changed to: " + name);
        }

        FGObjectData data;
        if (fgCat == FGCat.HANDLER || fgCat == FGCat.CONTROLLER) {
            HandlerData handlerData = new HandlerData();
            if (index != null && index.priority != null)
                handlerData.setPriority(index.priority);
            data = handlerData;
        } else data = new FGObjectData();
        data.setName(name);
        data.setOwner(owner);
        if (index != null && index.enabled != null)
            data.setEnabled(index.enabled);

        StringBuilder infoMessage = new StringBuilder();
        infoMessage.append("Foxguard object info loaded!  Category: \"").append(category).append("\",  ");
        infoMessage.append("Type: \"").append(type).append("\",  ");
        infoMessage.append("Name: \"").append(name).append("\",  ");
        if (isOwned) infoMessage.append("Owner: \"").append(ownerName).append("\",  ");
        infoMessage.append("Enabled: \"").append(data.isEnabled()).append("\"");
        logger.info(infoMessage.toString());

        IFGObject fgObject = null;
        try {
            fgObject = fgCat.loadInstance(directory, type, data);
        } catch (Exception e) {
            logger.error("There was an error creating the " + fgCat.lName, e);
        }

        if (fgObject == null) {
            logger.warn("The " + fgCat.lName + " was unable to be created.");
            if (FGConfigManager.getInstance().cleanupFiles()) {
                logger.warn("Cleaning up unused files");
                deleteDirectory(directory);
            }
        }

        return Optional.ofNullable(fgObject);
    }

    public GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        if(prettyPrint) builder.setPrettyPrinting();
        return builder;
    }

    public JsonWriter getJsonWriter(Writer out){
        JsonWriter writer = new JsonWriter(out);
        if(this.prettyPrint) writer.setIndent(this.gsonIndentString);

        return writer;
    }

    /*public String getGsonIndent() {
        return gsonIndentString;
    }*/



    private Path getFGDirectory() {
        if (fgDirectory != null) {
            constructDirectories(fgDirectory);
            return fgDirectory;
        }
        Path path = Sponge.getGame().getSavesDirectory();
        if (FGConfigManager.getInstance().saveInWorldFolder()) {
            path = path.resolve(Sponge.getServer().getDefaultWorldName());
        } else if (FGConfigManager.getInstance().useConfigFolder()) {
            path = FoxGuardMain.instance().getConfigDirectory();
        }
        path = path.resolve("foxguard");
        constructDirectories(path);
        fgDirectory = path;
        return path;
    }

    private Path getWorldDirectory(World world) {
        if (worldDirectories.containsKey(world)) {
            Path directory = worldDirectories.get(world);
            constructDirectories(directory);
            return directory;
        }
        FGConfigManager manager = FGConfigManager.getInstance();
        Path path;
        if (manager.saveWorldRegionsInWorldFolders()) {
            path = world.getDirectory();
            path = path.resolve("foxguard");
        } else {
            if (manager.useCustomDirectory()) {
                path = manager.customDirectory();
            } else {
                path = getFGDirectory();
            }
            path = path.resolve("worlds").resolve(world.getName());
        }
        constructDirectories(path);
        worldDirectories.put(world, path);
        return path;
    }

    public void constructDirectories(Path directory) {
        LinkedList<Path> stack = new LinkedList<>();
        Path dir = directory.normalize();
        while (true) {
            if (Files.notExists(dir) || !Files.isDirectory(dir)) {
                stack.push(dir);
                Path parent = dir.getParent();
                if (parent != null) {
                    dir = parent;
                    continue;
                }
            }
            break;
        }

        while (!stack.isEmpty()) {
            Path path = stack.pop();
            constructDirectory(path);
        }
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
        World world = null;
        boolean flag = false;
        if (object instanceof IWorldBound) {
            world = ((IWorldBound) object).getWorld();
            flag = true;
        }
        return getObjectDirectory(getObjectType(object),
                object.getOwner(),
                object.getName(),
                flag,
                world);
    }

    private Path getObjectDirectory(FGSObjectMeta meta, @Nullable World world) {
        FGCat fgCat = FGCat.from(meta.category);
        return getObjectDirectory(fgCat,
                meta.owner,
                meta.name,
                fgCat == FGCat.WORLDREGION,
                world);
    }

    private Path getObjectDirectory(FGCat fgCat, UUID owner, String name, boolean inWorld, @Nullable World world) {
        if (fgCat == null) fgCat = FGCat.OBJECT;
        Path dir;
        if (inWorld) {
            if (world == null)
                world = getDefaultWorld();
            dir = getWorldDirectory(world);
        } else {
            dir = getFGDirectory();
        }

        boolean ownerFirst = FGConfigManager.getInstance().ownerFirst();

        if (!ownerFirst) dir = dir.resolve(fgCat.pathName);
        if (owner != null && !owner.equals(SERVER_UUID)) {
            dir = dir.resolve("owners").resolve(owner.toString());
        }
        if (ownerFirst) dir = dir.resolve(fgCat.pathName);

        dir = dir.resolve(name.toLowerCase());

        constructDirectories(dir);
        return dir;
    }

    private World getDefaultWorld() {
        Server server = Sponge.getServer();
        World world = null;
        Optional<WorldProperties> worldPropertiesOpt = server.getDefaultWorld();
        if (worldPropertiesOpt.isPresent()) {
            WorldProperties worldProperties = worldPropertiesOpt.get();
            Optional<World> worldOpt = server.getWorld(worldProperties.getUniqueId());
            if (!worldOpt.isPresent())
                worldOpt = server.getWorld(worldProperties.getWorldName());
            if (worldOpt.isPresent())
                world = worldOpt.get();
        }
        if (world == null) {
            String worldName = server.getDefaultWorldName();
            Optional<World> worldOpt = server.getWorld(worldName);
            if (worldOpt.isPresent()) world = worldOpt.get();
        }
        if (world == null) {
            Collection<World> worlds = server.getWorlds();
            Iterator<World> worldIterator = worlds.iterator();
            if (worldIterator.hasNext()) {
                world = worldIterator.next();
            } else {
                logger.error("Could not find default world! There are no worlds loaded!");
            }
        }
        return world;
    }

    private boolean isGlobal(FGCat fgCat, String type, String name) {
        switch (fgCat) {
            case REGION:
                if (type.equals("sglobal") || name.equals("_sglobal")) return true;
                break;
            case WORLDREGION:
                if (type.equals("wglobal") || name.equals("_wglobal")) return true;
                break;
            case HANDLER:
                if (type.equals("global") || name.equals("_global")) return true;
                break;
        }
        return false;
    }

    private FGCat getObjectType(IFGObject object) {
        if (object instanceof IRegion) {
            if (object instanceof IWorldRegion) {
                return FGCat.WORLDREGION;
            }
            return FGCat.REGION;
        } else if (object instanceof IHandler) {
            if (object instanceof IController) {
                return FGCat.CONTROLLER;
            }
            return FGCat.HANDLER;
        }
        return FGCat.OBJECT;
    }

    private enum FGCat {
        REGION("regions") {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return FGManager.getInstance().isRegionNameAvailable(name, owner);
            }

            @Override
            public IFGObject loadInstance(Path directory, String type, FGObjectData data) {
                return FGFactoryManager.getInstance().createRegion(directory, type, data);
            }
        },
        WORLDREGION("wregions") {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                if (world == null)
                    return FGManager.getInstance().isWorldRegionNameAvailable(name, owner) == Tristate.TRUE;
                else return FGManager.getInstance().isWorldRegionNameAvailable(name, owner, world);
            }

            @Override
            public IFGObject loadInstance(Path directory, String type, FGObjectData data) {
                return FGFactoryManager.getInstance().createWorldRegion(directory, type, data);
            }
        },
        HANDLER("handlers") {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return FGManager.getInstance().isHandlerNameAvailable(name, owner);
            }

            @Override
            public IFGObject loadInstance(Path directory, String type, FGObjectData data) {
                HandlerData handlerData;
                if (data instanceof HandlerData) {
                    handlerData = (HandlerData) data;
                } else {
                    handlerData = new HandlerData(data, 0);
                }
                return FGFactoryManager.getInstance().createHandler(directory, type, handlerData);
            }
        },
        CONTROLLER(HANDLER.pathName) {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return HANDLER.isNameAvailable(name, owner, world);
            }

            @Override
            public IFGObject loadInstance(Path directory, String type, FGObjectData data) {
                HandlerData handlerData;
                if (data instanceof HandlerData) {
                    handlerData = (HandlerData) data;
                } else {
                    handlerData = new HandlerData(data, 0);
                }
                return FGFactoryManager.getInstance().createController(directory, type, handlerData);
            }
        },
        OBJECT("objects") {
            @Override
            public boolean isNameAvailable(String name, UUID owner, @Nullable World world) {
                return true;
            }

            @Override
            public IFGObject loadInstance(Path directory, String type, FGObjectData data) {
                return null;
            }
        };

        String lName = name().toLowerCase();
        String uName = FCCUtil.toCapitalCase(name());
        String pathName;

        FGCat(String pathName) {
            this.pathName = pathName;
        }

        public static FGCat from(String name) {
            for (FGCat type : values()) {
                if (type.lName.equals(name)) return type;
            }
            return null;
        }

        public String getIndexFile() {
            return pathName + ".foxcf";
        }

        public abstract boolean isNameAvailable(String name, UUID owner, @Nullable World world);

        public abstract IFGObject loadInstance(Path directory, String type, FGObjectData data);
    }

    /*private void setPrettyPrint(boolean prettyPrint) {
        if (this.prettyPrint == prettyPrint) return;
        this.prettyPrint = prettyPrint;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) builder.setPrettyPrinting();
        this.gson = builder.create();
    }*/
}
