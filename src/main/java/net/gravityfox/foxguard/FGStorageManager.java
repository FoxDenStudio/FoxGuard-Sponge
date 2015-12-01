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

package net.gravityfox.foxguard;

import net.gravityfox.foxguard.factory.FGFactoryManager;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.DeferredObject;
import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FGStorageManager {
    private static FGStorageManager instance;
    private List<String> markedForDeletion = new ArrayList<>();
    private List<DeferredObject> deferedObjects = new LinkedList<>();

    public static FGStorageManager getInstance() {
        if (instance == null) {
            instance = new FGStorageManager();
        }
        return instance;
    }

    public synchronized void initHandlers() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();

        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS HANDLERS (" +
                                "NAME VARCHAR(256), " +
                                "TYPE VARCHAR(256)," +
                                "PRIORITY INTEGER," +
                                "ENABLED BOOLEAN);");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public synchronized void initWorld(World world) {
        String dataBaseDir;
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS REGIONS (" +
                                "NAME VARCHAR(256), " +
                                "TYPE VARCHAR(256)," +
                                "ENABLED BOOLEAN);" +
                                "CREATE TABLE IF NOT EXISTS LINKAGES (" +
                                "REGION VARCHAR(256)," +
                                "HANDLER VARCHAR(256));");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadHandlers() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();

        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet handlerDataSet = statement.executeQuery("SELECT * FROM HANDLERS;")) {
                    while (handlerDataSet.next()) {
                        try {
                            String databaseDir = "jdbc:h2:./" +
                                    server.getDefaultWorld().get().getWorldName() + "/foxguard/handlers/" +
                                    handlerDataSet.getString("NAME");
                            DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                            try (Connection metaConn = source.getConnection()) {
                                try (Statement metaStatement = metaConn.createStatement()) {

                                    try (ResultSet metaTables = metaStatement.executeQuery(
                                            "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');")) {
                                        metaTables.first();
                                        if (metaTables.getInt(1) != 0) {
                                            try (ResultSet metaSet = metaStatement.executeQuery("SELECT * FROM FOXGUARD_META.METADATA;")) {
                                                metaSet.first();
                                                if (metaSet.getString("CATEGORY").equalsIgnoreCase("handler") &&
                                                        metaSet.getString("NAME").equalsIgnoreCase(handlerDataSet.getString("NAME")) &&
                                                        metaSet.getString("TYPE").equalsIgnoreCase(handlerDataSet.getString("TYPE")) &&
                                                        metaSet.getInt("PRIORITY") == handlerDataSet.getInt("PRIORITY") &&
                                                        metaSet.getBoolean("ENABLED") == handlerDataSet.getBoolean("ENABLED")) {
                                                    FGManager.getInstance().addHandler(
                                                            FGFactoryManager.getInstance().createHandler(
                                                                    source, handlerDataSet.getString("NAME"), handlerDataSet.getString("TYPE"),
                                                                    handlerDataSet.getInt("PRIORITY"), handlerDataSet.getBoolean("ENABLED")));
                                                } else if (FGConfigManager.getInstance().forceLoad()) {
                                                    FoxGuardMain.getInstance().getLogger().info("Mismatched database found. Attempting force load...");
                                                    if (metaSet.getString("CATEGORY").equalsIgnoreCase("region")) {
                                                        DeferredObject deferredRegion = new DeferredObject();
                                                        deferredRegion.category = "region";
                                                        deferredRegion.dataSource = source;
                                                        deferredRegion.databaseDir = databaseDir;
                                                        deferredRegion.listName = handlerDataSet.getString("NAME");
                                                        deferredRegion.metaName = metaSet.getString("NAME");
                                                        deferredRegion.type = metaSet.getString("TYPE");
                                                        deferredRegion.listWorld = null;
                                                        deferredRegion.metaWorld = metaSet.getString("WORLD");
                                                        deferredRegion.listEnabled = handlerDataSet.getBoolean("ENABLED");
                                                        deferredRegion.metaEnabled = metaSet.getBoolean("ENABLED");
                                                        this.deferedObjects.add(deferredRegion);
                                                    } else if (metaSet.getString("CATEGORY").equalsIgnoreCase("handler")) {
                                                        DeferredObject deferredHandler = new DeferredObject();
                                                        deferredHandler.category = "handler";
                                                        deferredHandler.dataSource = source;
                                                        deferredHandler.databaseDir = databaseDir;
                                                        deferredHandler.listName = handlerDataSet.getString("NAME");
                                                        deferredHandler.metaName = metaSet.getString("NAME");
                                                        deferredHandler.type = metaSet.getString("TYPE");
                                                        deferredHandler.priority = metaSet.getInt("PRIORITY");
                                                        deferredHandler.listEnabled = handlerDataSet.getBoolean("ENABLED");
                                                        deferredHandler.metaEnabled = metaSet.getBoolean("ENABLED");
                                                        this.deferedObjects.add(deferredHandler);
                                                    } else {
                                                        FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                                        markForDeletion(databaseDir);
                                                    }
                                                } else {
                                                    FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                                    markForDeletion(databaseDir);
                                                }
                                            }
                                        } else {
                                            FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                            markForDeletion(databaseDir);
                                        }
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadWorldRegions(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String worldDatabaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.getInstance().getDataSource(worldDatabaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet regionDataSet = statement.executeQuery("SELECT * FROM REGIONS;")) {
                    while (regionDataSet.next()) {
                        String databaseDir;
                        try {
                            databaseDir = worldDatabaseDir + "regions/" + regionDataSet.getString("NAME");
                            DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                            try (Connection metaConn = source.getConnection()) {
                                try (Statement metaStatement = metaConn.createStatement()) {
                                    try (ResultSet metaTables = metaStatement.executeQuery(
                                            "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');")) {
                                        metaTables.first();
                                        if (metaTables.getInt(1) != 0) {
                                            try (ResultSet metaSet = metaStatement.executeQuery("SELECT * FROM FOXGUARD_META.METADATA;")) {
                                                metaSet.first();
                                                if (metaSet.getString("CATEGORY").equalsIgnoreCase("region") &&
                                                        metaSet.getString("NAME").equalsIgnoreCase(regionDataSet.getString("NAME")) &&
                                                        metaSet.getString("TYPE").equalsIgnoreCase(regionDataSet.getString("TYPE")) &&
                                                        metaSet.getString("WORLD").equals(world.getName()) &&
                                                        metaSet.getBoolean("ENABLED") == regionDataSet.getBoolean("ENABLED")) {
                                                    FGManager.getInstance().addRegion(world,
                                                            FGFactoryManager.getInstance().createRegion(
                                                                    source,
                                                                    regionDataSet.getString("NAME"), regionDataSet.getString("TYPE"),
                                                                    regionDataSet.getBoolean("ENABLED")
                                                            )
                                                    );
                                                } else if (FGConfigManager.getInstance().forceLoad()) {
                                                    FoxGuardMain.getInstance().getLogger().info("Mismatched database found. Attempting force load...");
                                                    if (metaSet.getString("CATEGORY").equalsIgnoreCase("region")) {
                                                        DeferredObject deferredRegion = new DeferredObject();
                                                        deferredRegion.category = "region";
                                                        deferredRegion.dataSource = source;
                                                        deferredRegion.databaseDir = databaseDir;
                                                        deferredRegion.listName = regionDataSet.getString("NAME");
                                                        deferredRegion.metaName = metaSet.getString("NAME");
                                                        deferredRegion.type = metaSet.getString("TYPE");
                                                        deferredRegion.listWorld = world;
                                                        deferredRegion.metaWorld = metaSet.getString("WORLD");
                                                        deferredRegion.listEnabled = regionDataSet.getBoolean("ENABLED");
                                                        deferredRegion.metaEnabled = metaSet.getBoolean("ENABLED");
                                                        this.deferedObjects.add(deferredRegion);
                                                    } else if (metaSet.getString("CATEGORY").equalsIgnoreCase("handler")) {
                                                        DeferredObject deferredHandler = new DeferredObject();
                                                        deferredHandler.category = "handler";
                                                        deferredHandler.dataSource = source;
                                                        deferredHandler.databaseDir = databaseDir;
                                                        deferredHandler.listName = regionDataSet.getString("NAME");
                                                        deferredHandler.metaName = metaSet.getString("NAME");
                                                        deferredHandler.type = metaSet.getString("TYPE");
                                                        deferredHandler.priority = metaSet.getInt("PRIORITY");
                                                        deferredHandler.listEnabled = regionDataSet.getBoolean("ENABLED");
                                                        deferredHandler.metaEnabled = metaSet.getBoolean("ENABLED");
                                                        this.deferedObjects.add(deferredHandler);
                                                    } else {
                                                        FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                                        markForDeletion(databaseDir);
                                                    }
                                                } else {
                                                    FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                                    markForDeletion(databaseDir);
                                                }
                                            }
                                        } else {
                                            FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                            markForDeletion(databaseDir);
                                        }
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            FoxGuardMain.getInstance().getLogger().info("Unable to load Region in world \"" + world.getName() + "\". Perhaps the database is corrupted?");
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadWorldLinks(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet linkSet = statement.executeQuery("SELECT  * FROM LINKAGES;")) {
                    while (linkSet.next()) {
                        FGManager.getInstance().link(world, linkSet.getString("REGION"), linkSet.getString("HANDLER"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadLinks() {
        FoxGuardMain.getInstance().getGame().getServer().getWorlds().forEach(this::loadWorldLinks);
    }

    public synchronized void writeHandlers() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DELETE FROM HANDLERS;");
                for (IHandler handler : FGManager.getInstance().getHandlerListCopy()) {
                    statement.addBatch("INSERT INTO HANDLERS(NAME, TYPE, PRIORITY, ENABLED) VALUES ('" +
                            handler.getName() + "', '" +
                            handler.getUniqueTypeString() + "', " +
                            handler.getPriority() + ", " +
                            (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (IHandler handler : FGManager.getInstance().getHandlerListCopy()) {
            try {
                DataSource source = FoxGuardMain.getInstance().getDataSource(
                        "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() +
                                "/foxguard/handlers/" + handler.getName());
                handler.writeToDatabase(source);
                try (Connection conn = source.getConnection()) {
                    try (Statement statement = conn.createStatement()) {
                        statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                        statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                                "CATEGORY VARCHAR(16), " +
                                "NAME VARCHAR(256), " +
                                "TYPE VARCHAR(256), " +
                                "PRIORITY INTEGER," +
                                "ENABLED BOOLEAN);");
                        statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                        statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, PRIORITY, ENABLED) VALUES (" +
                                "'handler', '" +
                                handler.getName() + "', '" +
                                handler.getUniqueTypeString() + "', " +
                                handler.getPriority() + ", " +
                                (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                        statement.executeBatch();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void writeWorld(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DELETE FROM REGIONS; DELETE FROM LINKAGES;");
                for (IRegion region : FGManager.getInstance().getRegionsListCopy(world)) {
                    statement.addBatch("INSERT INTO REGIONS(NAME, TYPE, ENABLED) VALUES ('" +
                            region.getName() + "', '" +
                            region.getUniqueTypeString() + "', " +
                            (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                    for (IHandler handler : region.getHandlersCopy()) {
                        statement.addBatch("INSERT INTO LINKAGES(REGION, HANDLER) VALUES ('" +
                                region.getName() + "', '" +
                                handler.getName() + "');");
                    }
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (IRegion region : FGManager.getInstance().getRegionsListCopy(world)) {
            try {
                DataSource source = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "regions/" + region.getName());
                region.writeToDatabase(source);
                try (Connection conn = source.getConnection()) {
                    try (Statement statement = conn.createStatement()) {
                        statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                        statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                                "CATEGORY VARCHAR(16), " +
                                "NAME VARCHAR(256), " +
                                "TYPE VARCHAR(256), " +
                                "WORLD VARCHAR(256)," +
                                "ENABLED BOOLEAN);");
                        statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                        statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, WORLD, ENABLED) VALUES (" +
                                "'region', '" +
                                region.getName() + "', '" +
                                region.getUniqueTypeString() + "', '" +
                                region.getWorld().getName() + "', " +
                                (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                        statement.executeBatch();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void update(IFGObject object) {
        if (object instanceof IRegion)
            updateRegion((IRegion) object);
        else if (object instanceof IHandler)
            updateHandler((IHandler) object);
    }

    public synchronized void updateRegion(IRegion region) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        World world = region.getWorld();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        try {
            DataSource source = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "regions/" + region.getName());
            region.writeToDatabase(source);
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                    statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                            "CATEGORY VARCHAR(16), " +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256), " +
                            "WORLD VARCHAR(256)," +
                            "ENABLED BOOLEAN);");
                    statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                    statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, WORLD, ENABLED) VALUES (" +
                            "'region', '" +
                            region.getName() + "', '" +
                            region.getUniqueTypeString() + "', '" +
                            region.getWorld().getName() + "', " +
                            (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                    statement.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateLists();
    }

    public synchronized void updateHandler(IHandler handler) {
        if (!FoxGuardMain.getInstance().isLoaded()) return;
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try {
            DataSource source = FoxGuardMain.getInstance().getDataSource(
                    "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() +
                            "/foxguard/handlers/" + handler.getName());
            handler.writeToDatabase(source);
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                    statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                            "CATEGORY VARCHAR(16), " +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256), " +
                            "PRIORITY INTEGER," +
                            "ENABLED BOOLEAN);");
                    statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                    statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, PRIORITY, ENABLED) VALUES (" +
                            "'handler', '" +
                            handler.getName() + "', '" +
                            handler.getUniqueTypeString() + "', " +
                            handler.getPriority() + ", " +
                            (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                    statement.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateLists();
    }

    public synchronized void updateLists() {
        if (!FoxGuardMain.getInstance().isLoaded()) return;
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        for (World world : FoxGuardMain.getInstance().getGame().getServer().getWorlds()) {
            String dataBaseDir;
            if (world.getProperties().equals(server.getDefaultWorld().get())) {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
            } else {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
            }
            try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.addBatch("DELETE FROM REGIONS; DELETE FROM LINKAGES;");
                    for (IRegion region : FGManager.getInstance().getRegionsListCopy(world)) {
                        statement.addBatch("INSERT INTO REGIONS(NAME, TYPE, ENABLED) VALUES ('" +
                                region.getName() + "', '" +
                                region.getUniqueTypeString() + "', " +
                                (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                        for (IHandler handler : region.getHandlersCopy()) {
                            statement.addBatch("INSERT INTO LINKAGES(REGION, HANDLER) VALUES ('" +
                                    region.getName() + "', '" +
                                    handler.getName() + "');");
                        }
                    }
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DELETE FROM HANDLERS;");
                for (IHandler handler : FGManager.getInstance().getHandlerListCopy()) {
                    statement.addBatch("INSERT INTO HANDLERS(NAME, TYPE, PRIORITY, ENABLED) VALUES ('" +
                            handler.getName() + "', '" +
                            handler.getUniqueTypeString() + "', " +
                            handler.getPriority() + ", " +
                            (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void markForDeletion(String databaseDir) {
        if (FGConfigManager.getInstance().purgeDatabases()) {
            FoxGuardMain.getInstance().getLogger().info("Clearing database " + databaseDir + "...");
            try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("DROP ALL OBJECTS;");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (!markedForDeletion.contains(databaseDir)) markedForDeletion.add(databaseDir);
    }

    public synchronized void markForDeletion(IFGObject object) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String databaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/regions/";
            } else {
                databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            databaseDir += region.getName();
            markForDeletion(databaseDir);
        } else if (object instanceof IHandler) {
            String databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/handlers/" + object.getName();
            markForDeletion(databaseDir);
        }
    }

    public synchronized void unmarkForDeletion(String databaseDir) {
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("DROP ALL OBJECTS;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        markedForDeletion.remove(databaseDir);
    }

    public synchronized void unmarkForDeletion(IFGObject object) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String databaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/regions/";
            } else {
                databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            databaseDir += region.getName();
            unmarkForDeletion(databaseDir);
        } else if (object instanceof IHandler) {
            if (server.getDefaultWorld().isPresent()) {
                unmarkForDeletion("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/handlers/" + object.getName());
            }
        }
    }

    public synchronized void purgeDatabases() {
        if (FGConfigManager.getInstance().purgeDatabases()) {
            FoxGuardMain.getInstance().getLogger().info("Purging databases...");
            for (String databaseDir : markedForDeletion) {
                FoxGuardMain.getInstance().getLogger().info("Deleting database " + databaseDir + "...");
                try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
                    try (Statement statement = conn.createStatement()) {
                        statement.execute("DROP ALL OBJECTS DELETE FILES;");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void resolveDeferredObjects() {
        for (DeferredObject o : deferedObjects) {
            try {
                IFGObject result = o.resolve();
                if (result == null)
                    FoxGuardMain.getInstance().getLogger().info("Unable to resolve deferred object:\n" + o.toString());
            } catch (SQLException e) {
                FoxGuardMain.getInstance().getLogger().info("Unable to resolve deferred object:\n" + o.toString());
                e.printStackTrace();
            }
        }
    }
}


