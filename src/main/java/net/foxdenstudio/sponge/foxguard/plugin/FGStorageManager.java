package net.foxdenstudio.sponge.foxguard.plugin;

import net.foxdenstudio.sponge.foxcore.plugin.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by Fox on 4/6/2016.
 */
public final class FGStorageManager {
    private static FGStorageManager instance;
    private final Logger logger = FoxGuardMain.instance().getLogger();
    private final Set<LoadEntry> loaded = new HashSet<>();
    private final Path directory = getDirectory();
    private final Map<World, Path> worldDirectories = new CacheMap<>((k, m) -> {
        if (k instanceof World) {
            Path dir = getWorldDirectory((World) k);
            m.put((World) k, dir);
            return dir;
        } else return null;
    });

    public static FGStorageManager getInstance() {
        if (instance == null) instance = new FGStorageManager();
        return instance;
    }

    public synchronized void saveRegions() {
        try (DB mainDB = DBMaker.fileDB(directory.resolve("regions.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();
            Map<String, String> linksMap = mainDB.hashMap("links", Serializer.STRING, Serializer.STRING).make();

            Path dir = directory.resolve("regions");
            constructDirectory(dir);
            FGManager.getInstance().getRegions().forEach(fgObject -> {
                String name = fgObject.getName();
                Path singleDir = dir.resolve(name.toLowerCase());
                logger.info("Saving region \"" + name + "\" in directory: " + singleDir);
                constructDirectory(singleDir);
                try {
                    fgObject.save(singleDir);
                } catch (Exception e) {
                    logger.error("There was an error while saving region \"" + name + "\"!", e);
                }

                logger.info("Saving metadata");
                try (DB metaDB = DBMaker.fileDB(singleDir.resolve("metadata.db").normalize().toString()).make()) {
                    Atomic.String metaName = metaDB.atomicString("name").make();
                    Atomic.String metaCategory = metaDB.atomicString("category").make();
                    Atomic.String metaType = metaDB.atomicString("type").make();
                    Atomic.Boolean metaEnabled = metaDB.atomicBoolean("enabled").make();
                    metaName.set(name);
                    metaCategory.set(FGUtil.getCategory(fgObject));
                    metaType.set(fgObject.getUniqueTypeString());
                    metaEnabled.set(fgObject.isEnabled());
                }

                mainMap.put(name, FGUtil.getCategory(fgObject));
                typeMap.put(name, fgObject.getUniqueTypeString());
                enabledMap.put(name, fgObject.isEnabled());
                linksMap.put(name, serializeHandlerList(fgObject.getHandlers()));
            });
        }
    }

    public synchronized void saveWorldRegions(World world) {
        try (DB mainDB = DBMaker.fileDB(worldDirectories.get(world).resolve("wregions.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();
            Map<String, String> linksMap = mainDB.hashMap("links", Serializer.STRING, Serializer.STRING).make();

            Path dir = worldDirectories.get(world).resolve("regions");
            constructDirectory(dir);
            FGManager.getInstance().getRegions().forEach(fgObject -> {
                String name = fgObject.getName();
                Path singleDir = dir.resolve(name.toLowerCase());
                logger.info("Saving world region \"" + name + "\" in directory: " + singleDir);
                constructDirectory(singleDir);
                try {
                    fgObject.save(singleDir);
                } catch (Exception e) {
                    logger.error("There was an error while saving world region \"" + name + "\" in world \"" + world.getName() + "\"!", e);
                }

                logger.info("Saving metadata");
                try (DB metaDB = DBMaker.fileDB(singleDir.resolve("metadata.db").normalize().toString()).make()) {
                    Atomic.String metaName = metaDB.atomicString("name").make();
                    Atomic.String metaCategory = metaDB.atomicString("category").make();
                    Atomic.String metaType = metaDB.atomicString("type").make();
                    Atomic.Boolean metaEnabled = metaDB.atomicBoolean("enabled").make();
                    metaName.set(name);
                    metaCategory.set(FGUtil.getCategory(fgObject));
                    metaType.set(fgObject.getUniqueTypeString());
                    metaEnabled.set(fgObject.isEnabled());
                }

                mainMap.put(name, FGUtil.getCategory(fgObject));
                typeMap.put(name, fgObject.getUniqueTypeString());
                enabledMap.put(name, fgObject.isEnabled());
                linksMap.put(name, serializeHandlerList(fgObject.getHandlers()));
            });
        }
    }

    public synchronized void saveHandlers() {
        try (DB mainDB = DBMaker.fileDB(directory.resolve("handlers.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();
            Map<String, Integer> priorityMap = mainDB.hashMap("priority", Serializer.STRING, Serializer.INTEGER).make();

            Path dir = directory.resolve("handlers");
            constructDirectory(dir);
            FGManager.getInstance().getHandlers().forEach(fgObject -> {
                String name = fgObject.getName();
                Path singleDir = dir.resolve(name.toLowerCase());
                logger.info("Saving handler \"" + name + "\" in directory: " + singleDir);
                constructDirectory(singleDir);
                try {
                    fgObject.save(singleDir);
                } catch (Exception e) {
                    logger.error("There was an error while saving handler \"" + name + "\"!", e);
                }

                logger.info("Saving metadata");
                try (DB metaDB = DBMaker.fileDB(singleDir.resolve("metadata.db").normalize().toString()).make()) {
                    Atomic.String metaName = metaDB.atomicString("name").make();
                    Atomic.String metaCategory = metaDB.atomicString("category").make();
                    Atomic.String metaType = metaDB.atomicString("type").make();
                    Atomic.Boolean metaEnabled = metaDB.atomicBoolean("enabled").make();
                    Atomic.Integer metaPriority = metaDB.atomicInteger("priority").make();
                    metaName.set(name);
                    metaCategory.set(FGUtil.getCategory(fgObject));
                    metaType.set(fgObject.getUniqueTypeString());
                    metaEnabled.set(fgObject.isEnabled());
                    metaPriority.set(fgObject.getPriority());
                }

                mainMap.put(name, FGUtil.getCategory(fgObject));
                typeMap.put(name, fgObject.getUniqueTypeString());
                enabledMap.put(name, fgObject.isEnabled());
                priorityMap.put(name, fgObject.getPriority());
            });
        }
    }

    public synchronized void loadRegions() {
        try (DB mainDB = DBMaker.fileDB(directory.resolve("regions.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();

            Path dir = directory.resolve("regions");
            mainMap.entrySet().forEach((entry) -> {
                String name = entry.getKey();
                Path singleDir = dir.resolve(name.toLowerCase());
                Path metaDataFile = singleDir.resolve("metadata.db");
                logger.info("Loading region \"" + name + "\" from " + singleDir);
                if (Files.exists(metaDataFile) && !Files.isDirectory(metaDataFile)) {
                    String category;
                    String type;
                    Boolean enabled;
                    try (DB metaDB = DBMaker.fileDB(metaDataFile.normalize().toString()).make()) {
                        category = metaDB.exists("category") ? metaDB.atomicString("category").make().get() : entry.getValue();
                        type = metaDB.exists("type") ? metaDB.atomicString("type").make().get() : typeMap.get(name);
                        enabled = metaDB.exists("enabled") ? metaDB.atomicBoolean("enabled").make().get() : enabledMap.get(name);
                    }
                    logger.info("Region info loaded! Name: " + name + "Category: " + category + " Type: " + type + " Enabled: " + enabled);
                    if (category == null) category = "";
                    if (type == null) type = "";
                    IRegion object = null;
                    try {
                        if (category.equalsIgnoreCase("region"))
                            object = FGFactoryManager.getInstance().createRegion(singleDir, name, type, enabled);
                        else logger.warn("Category \"" + category + "\" is invalid!");
                    } catch (Exception e) {
                        logger.error("There was an error creating the region!", e);
                    }
                    if (object != null) {
                        FGManager.getInstance().addRegion(object);
                        loaded.add(new LoadEntry(object));
                    } else {
                        logger.warn("A region was unable to be created. Either the metadata is incorrect, or there is no longer a factory available to create it.");
                        if (FGConfigManager.getInstance().cleanupFiles()) {
                            logger.warn("Cleaning up unused files");
                            System.gc();
                            System.runFinalization();
                            deleteDirectory(singleDir);
                        }
                    }
                } else {
                    logger.warn("Metadata file not found! Skipping...");
                    if (Files.exists(singleDir)) {
                        if (isEmptyDirectory(singleDir)) {
                            logger.warn("Empty region directory found. Deleting...");
                            try {
                                Files.delete(singleDir);
                            } catch (IOException e) {
                                logger.error("There was an error deleting the region directory: " + singleDir, e);
                            }
                        } else {
                            if (FGConfigManager.getInstance().cleanupFiles()) {
                                logger.warn("Cleaning up unused files");
                                deleteDirectory(singleDir);
                            }
                        }
                    }
                }
            });

        }
    }

    public synchronized void loadWorldRegions(World world) {
        try (DB mainDB = DBMaker.fileDB(worldDirectories.get(world).resolve("wregions.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();

            Path dir = worldDirectories.get(world).resolve("regions");
            mainMap.entrySet().forEach((entry) -> {
                String name = entry.getKey();
                Path singleDir = dir.resolve(name.toLowerCase());
                Path metaDataFile = singleDir.resolve("metadata.db");
                logger.info("Loading world region \"" + name + "\" from " + singleDir);
                if (Files.exists(metaDataFile) && !Files.isDirectory(metaDataFile)) {
                    String category;
                    String type;
                    Boolean enabled;
                    try (DB metaDB = DBMaker.fileDB(metaDataFile.normalize().toString()).make()) {
                        category = metaDB.exists("category") ? metaDB.atomicString("category").make().get() : entry.getValue();
                        type = metaDB.exists("type") ? metaDB.atomicString("type").make().get() : typeMap.get(name);
                        enabled = metaDB.exists("enabled") ? metaDB.atomicBoolean("enabled").make().get() : enabledMap.get(name);
                    }
                    logger.info("World region info loaded! Name: " + name + "Category: " + category + " Type: " + type + " Enabled: " + enabled);
                    if (category == null) category = "";
                    if (type == null) type = "";
                    IWorldRegion object = null;
                    try {
                        if (category.equalsIgnoreCase("region"))
                            object = FGFactoryManager.getInstance().createWorldRegion(singleDir, name, type, enabled);
                        else logger.warn("Category \"" + category + "\" is invalid!");
                    } catch (Exception e) {
                        logger.error("There was an error creating the world region!", e);
                    }
                    if (object != null) {
                        FGManager.getInstance().addWorldRegion(world, object);
                        loaded.add(new LoadEntry(object));
                    } else {
                        logger.warn("A world region was unable to be created. Either the metadata is incorrect, or there is no longer a factory available to create it.");
                        if (FGConfigManager.getInstance().cleanupFiles()) {
                            logger.warn("Cleaning up unused files");
                            System.gc();
                            System.runFinalization();
                            deleteDirectory(singleDir);
                        }
                    }
                } else {
                    logger.warn("Metadata file not found! Skipping...");
                    if (Files.exists(singleDir)) {
                        if (isEmptyDirectory(singleDir)) {
                            logger.warn("Empty world region directory found. Deleting...");
                            try {
                                Files.delete(singleDir);
                            } catch (IOException e) {
                                logger.error("There was an error deleting the world region directory: " + singleDir, e);
                            }
                        } else {
                            if (FGConfigManager.getInstance().cleanupFiles()) {
                                logger.warn("Cleaning up unused files");
                                deleteDirectory(singleDir);
                            }
                        }
                    }
                }
            });

        }
    }

    public synchronized void loadHandlers() {
        try (DB mainDB = DBMaker.fileDB(directory.resolve("handlers.db").normalize().toString()).make()) {
            Map<String, String> mainMap = mainDB.hashMap("main", Serializer.STRING, Serializer.STRING).make();
            Map<String, String> typeMap = mainDB.hashMap("types", Serializer.STRING, Serializer.STRING).make();
            Map<String, Boolean> enabledMap = mainDB.hashMap("enabled", Serializer.STRING, Serializer.BOOLEAN).make();
            Map<String, Integer> priorityMap = mainDB.hashMap("priority", Serializer.STRING, Serializer.INTEGER).make();

            Path dir = directory.resolve("handlers");
            mainMap.entrySet().forEach((entry) -> {
                String name = entry.getKey();
                Path singleDir = dir.resolve(name.toLowerCase());
                Path metaDataFile = singleDir.resolve("metadata.db");
                logger.info("Loading handler \"" + name + "\" from " + singleDir);
                if (Files.exists(metaDataFile) && !Files.isDirectory(metaDataFile)) {
                    String category;
                    String type;
                    Boolean enabled;
                    Integer priority;
                    try (DB metaDB = DBMaker.fileDB(metaDataFile.normalize().toString()).make()) {
                        category = metaDB.exists("category") ? metaDB.atomicString("category").make().get() : entry.getValue();
                        type = metaDB.exists("type") ? metaDB.atomicString("type").make().get() : typeMap.get(name);
                        enabled = metaDB.exists("enabled") ? metaDB.atomicBoolean("enabled").make().get() : enabledMap.get(name);
                        priority = metaDB.exists("priority") ? metaDB.atomicInteger("priority").make().get() : priorityMap.get(name);
                    }
                    logger.info("Handler info loaded! Name: " + name + "Category: " + category + " Type: " + type + " Enabled: " + enabled + " Priority: " + priority);
                    if (category == null) category = "";
                    if (type == null) type = "";
                    IHandler object = null;
                    try {
                        if (category.equalsIgnoreCase("handler"))
                            object = FGFactoryManager.getInstance().createHandler(singleDir, name, type, enabled, priority);
                        else if (category.equalsIgnoreCase("controller"))
                            object = FGFactoryManager.getInstance().createController(singleDir, name, type, enabled, priority);
                        else logger.warn("Category \"" + category + "\" is invalid!");
                    } catch (Exception e) {
                        logger.error("There was an error creating the handler!", e);
                    }
                    if (object != null) {
                        FGManager.getInstance().addHandler(object);
                        loaded.add(new LoadEntry(object));
                    } else {
                        logger.warn("A handler was unable to be created. Either the metadata is incorrect, or there is no longer a factory available to create it.");
                        if (FGConfigManager.getInstance().cleanupFiles()) {
                            logger.warn("Cleaning up unused files");
                            System.gc();
                            System.runFinalization();
                            deleteDirectory(singleDir);
                        }
                    }
                } else {
                    logger.warn("Metadata file not found! Skipping...");
                    if (Files.exists(singleDir)) {
                        if (isEmptyDirectory(singleDir)) {
                            logger.warn("Empty handler directory found. Deleting...");
                            try {
                                Files.delete(singleDir);
                            } catch (IOException e) {
                                logger.error("There was an error deleting the handler directory: " + singleDir, e);
                            }
                        } else {
                            if (FGConfigManager.getInstance().cleanupFiles()) {
                                logger.warn("Cleaning up unused files");
                                deleteDirectory(singleDir);
                            }
                        }
                    }
                }
            });

        }
    }

    public synchronized void loadGlobalHandler() {
        Path path;
        constructDirectory(path = directory.resolve("handlers"));
        constructDirectory(path = path.resolve(GlobalHandler.NAME));
        FGManager.getInstance().getGlobalHandler().load(path);
    }

    public synchronized void loadLinks() {
        loadRegionLinks();
        Sponge.getServer().getWorlds().forEach(this::loadWorldRegionLinks);
        loadControllerLinks();
    }

    public synchronized void loadRegionLinks() {

    }

    public synchronized void loadWorldRegionLinks(World world) {
    }

    public synchronized void loadControllerLinks() {

    }

    public synchronized void addObject(IFGObject object) {
    }

    public synchronized void removeObject(IFGObject object) {
    }

    private Path getDirectory() {
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
            path = path.resolve(Sponge.getServer().getDefaultWorldName());
            if (!Sponge.getServer().getDefaultWorld().get().equals(world.getProperties())) {
                path = path.resolve(world.getName());
            }
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
        FoxGuardMain.instance().getLogger().info("Deleting directory: " + directory);
        if (Files.exists(directory) && Files.isDirectory(directory))
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            logger.error("There was an error deleting the file: " + file, e);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc == null) {
                            try {
                                Files.delete(dir);
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

    private void constructDirectory(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                logger.info("Created directory :" + directory);
            } catch (IOException e) {
                logger.error("There was an error creating the directory: " + directory, e);
            }
        } else if (!Files.isDirectory(directory)) {
            logger.warn("There is a file at " + directory + " where a directory was expected. Deleting and replacing with a directory...");
            try {
                Files.delete(directory);
                try {
                    Files.createDirectory(directory);
                    logger.info("Created directory :" + directory);
                } catch (IOException e) {
                    logger.error("Error creating the directory: " + directory, e);
                }
            } catch (IOException e) {
                logger.error("Error deleting the file: " + directory, e);
            }

        }
    }

    private boolean isEmptyDirectory(Path directory) {
        if (!Files.exists(directory)) return true;
        if (!Files.isDirectory(directory)) return false;
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(directory);
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            logger.error("Could not read contents of directory: " + directory, e);
            return false;
        }
    }

    private String serializeHandlerList(Collection<IHandler> handlers) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<IHandler> it = handlers.iterator(); it.hasNext(); ) {
            builder.append(it.next());
            if (it.hasNext()) builder.append(",");
        }
        return builder.toString();
    }

    private static class LoadEntry {
        public final String name;
        public final Type type;
        public final String world;

        public LoadEntry(String name, Type type, String world) {
            this.name = name;
            this.type = type;
            this.world = world;
        }

        public LoadEntry(IFGObject object) {
            name = object.getName();
            if (object instanceof IWorldRegion) {
                type = Type.WREGION;
                world = ((IWorldRegion) object).getWorld().getName();
            } else if (object instanceof IRegion) {
                type = Type.REGION;
                world = "";
            } else if (object instanceof IHandler) {
                type = Type.HANDLER;
                world = "";
            } else throw new IllegalArgumentException("Object is not of a valid subtype!");
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LoadEntry) {
                LoadEntry entry = (LoadEntry) obj;
                return name.equalsIgnoreCase(entry.name) &&
                        type.equals(entry.type) &&
                        world.equalsIgnoreCase(entry.world);
            } else return false;
        }

        @Override
        public int hashCode() {
            return name.toLowerCase().hashCode() + type.hashCode() + world.toLowerCase().hashCode();
        }

        public enum Type {
            REGION, WREGION, HANDLER
        }
    }
}
