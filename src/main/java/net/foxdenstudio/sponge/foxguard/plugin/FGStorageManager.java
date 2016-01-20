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

import net.foxdenstudio.sponge.foxguard.plugin.handler.GlobalHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.DeferredObject;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class FGStorageManager {
    private static FGStorageManager instance;
    private final List<String> markedForDeletion = new ArrayList<>();
    private final List<DeferredObject> deferedObjects = new ArrayList<>();

    public static FGStorageManager getInstance() {
        if (instance == null) {
            instance = new FGStorageManager();
        }
        return instance;
    }

    public synchronized void initHandlers() {
        Server server = Sponge.getGame().getServer();

        try (Connection conn = FoxGuardMain.instance().getDataSource("jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/foxguard").getConnection()) {
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
        Server server = Sponge.getGame().getServer();
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.instance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
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
        Server server = Sponge.getGame().getServer();

        try (Connection conn = FoxGuardMain.instance().getDataSource("jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet handlerDataSet = statement.executeQuery("SELECT * FROM HANDLERS;")) {
                    while (handlerDataSet.next()) {
                        try {
                            String databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName())
                                    + "/foxguard/handlers/" + handlerDataSet.getString("NAME");
                            DataSource source = FoxGuardMain.instance().getDataSource(databaseDir);
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
                                                    FoxGuardMain.instance().logger().info("Mismatched database found. Attempting force load...");
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
                                                        FoxGuardMain.instance().logger().warn("Found potentially corrupted database.");
                                                        markForDeletion(databaseDir);
                                                    }
                                                } else {
                                                    FoxGuardMain.instance().logger().warn("Found potentially corrupted database.");
                                                    markForDeletion(databaseDir);
                                                }
                                            }
                                        } else {
                                            FoxGuardMain.instance().logger().warn("Found potentially corrupted database.");
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
        Server server = Sponge.getGame().getServer();
        String worldDatabaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            worldDatabaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
        } else {
            worldDatabaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.instance().getDataSource(worldDatabaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet regionDataSet = statement.executeQuery("SELECT * FROM REGIONS;")) {
                    while (regionDataSet.next()) {
                        String databaseDir;
                        try {
                            databaseDir = worldDatabaseDir + "regions/" + regionDataSet.getString("NAME");
                            DataSource source = FoxGuardMain.instance().getDataSource(databaseDir);
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
                                                    FoxGuardMain.instance().logger().info("Mismatched database found. Attempting force load...");
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
                                                        FoxGuardMain.instance().logger().warn("Found potentially corrupted database. 1");
                                                        markForDeletion(databaseDir);
                                                    }
                                                } else {
                                                    FoxGuardMain.instance().logger().warn("Found potentially corrupted database. 2");
                                                    markForDeletion(databaseDir);
                                                }
                                            }
                                        } else {
                                            FoxGuardMain.instance().logger().warn("Found potentially corrupted database. 3");
                                            markForDeletion(databaseDir);
                                        }
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            FoxGuardMain.instance().logger().error("Unable to load Region in world \"" + world.getName() + "\". Perhaps the database is corrupted?");
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
        Server server = Sponge.getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.instance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
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
        Sponge.getGame().getServer().getWorlds().forEach(this::loadWorldLinks);
    }

    public synchronized void writeHandlers() {
        Server server = Sponge.getGame().getServer();
        try (Connection conn = FoxGuardMain.instance().getDataSource("jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DROP TABLE IF EXISTS HANDLERS;");
                statement.addBatch(
                        "CREATE TABLE HANDLERS (" +
                                "NAME VARCHAR(256), " +
                                "TYPE VARCHAR(256)," +
                                "PRIORITY INTEGER," +
                                "ENABLED BOOLEAN);");
                for (IHandler handler : FGManager.getInstance().getHandlerList()) {
                    if (handler.autoSave()) {
                        statement.addBatch("INSERT INTO HANDLERS(NAME, TYPE, PRIORITY, ENABLED) VALUES ('" +
                                handler.getName() + "', '" +
                                handler.getUniqueTypeString() + "', " +
                                handler.getPriority() + ", " +
                                (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                    }
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        FGManager.getInstance().getHandlerList().stream().filter(IFGObject::autoSave).forEach(handler -> {
            try {
                DataSource source = FoxGuardMain.instance().getDataSource(
                        "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) +
                                "/foxguard/handlers/" + handler.getName());
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
                        handler.writeToDatabase(source);
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
                FoxGuardMain.instance().logger().error("FAILED TO SAVE HANDLER \"" + handler.getName() + "\"!");
            }
        });
    }

    public synchronized void writeWorld(World world) {
        Server server = Sponge.getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.instance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DELETE FROM REGIONS; DELETE FROM LINKAGES;");
                for (IRegion region : FGManager.getInstance().getRegionsList(world)) {
                    if (region.autoSave()) {
                        statement.addBatch("INSERT INTO REGIONS(NAME, TYPE, ENABLED) VALUES ('" +
                                region.getName() + "', '" +
                                region.getUniqueTypeString() + "', " +
                                (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                        for (IHandler handler : region.getHandlers()) {
                            statement.addBatch("INSERT INTO LINKAGES(REGION, HANDLER) VALUES ('" +
                                    region.getName() + "', '" +
                                    handler.getName() + "');");
                        }
                    }
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FGManager.getInstance().getRegionsList(world).stream().filter(IFGObject::autoSave).forEach(region -> {
            try {
                DataSource source = FoxGuardMain.instance().getDataSource(dataBaseDir + "regions/" + region.getName());
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
                        region.writeToDatabase(source);
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
                FoxGuardMain.instance().logger().error("FAILED TO SAVE REGION \"" + region.getName() + "\"!");
            }
        });
    }

    public synchronized void update(IFGObject object) {
        if (object instanceof IRegion)
            updateRegion((IRegion) object);
        else if (object instanceof IHandler)
            updateHandler((IHandler) object);
    }

    public synchronized void updateRegion(IRegion region) {
        if (!region.autoSave()) return;
        Server server = Sponge.getGame().getServer();
        World world = region.getWorld();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
        }
        try {
            DataSource source = FoxGuardMain.instance().getDataSource(dataBaseDir + "regions/" + region.getName());
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
                    region.writeToDatabase(source);
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
            FoxGuardMain.instance().logger().error("FAILED TO UPDATE SAVE DATABASE FOR REGION \"" + region.getName() + "\"!");
        }
        updateLists();
    }

    public synchronized void updateHandler(IHandler handler) {
        if (!handler.autoSave()) return;
        if (!FoxGuardMain.instance().isLoaded()) return;
        Server server = Sponge.getGame().getServer();
        try {
            DataSource source = FoxGuardMain.instance().getDataSource(
                    "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) +
                            "/foxguard/handlers/" + handler.getName());
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
                    handler.writeToDatabase(source);
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
            FoxGuardMain.instance().logger().error("FAILED TO UPDATE SAVE DATABASE FOR HANDLER \"" + handler.getName() + "\"!");
        }
        updateLists();
    }

    private synchronized void updateLists() {
        if (!FoxGuardMain.instance().isLoaded()) return;
        Server server = Sponge.getGame().getServer();
        for (World world : Sponge.getGame().getServer().getWorlds()) {
            String dataBaseDir;
            if (world.getProperties().equals(server.getDefaultWorld().get())) {
                dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/";
            } else {
                dataBaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + world.getName() + "/foxguard/";
            }
            try (Connection conn = FoxGuardMain.instance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.addBatch("DELETE FROM REGIONS; DELETE FROM LINKAGES;");
                    for (IRegion region : FGManager.getInstance().getRegionsList(world)) {
                        if (region.autoSave()) {
                            statement.addBatch("INSERT INTO REGIONS(NAME, TYPE, ENABLED) VALUES ('" +
                                    region.getName() + "', '" +
                                    region.getUniqueTypeString() + "', " +
                                    (region.isEnabled() ? "TRUE" : "FALSE") + ");");
                            for (IHandler handler : region.getHandlers()) {
                                statement.addBatch("INSERT INTO LINKAGES(REGION, HANDLER) VALUES ('" +
                                        region.getName() + "', '" +
                                        handler.getName() + "');");
                            }
                        }
                    }
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try (Connection conn = FoxGuardMain.instance().getDataSource("jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/foxguard").getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.addBatch("DELETE FROM HANDLERS;");
                for (IHandler handler : FGManager.getInstance().getHandlerList()) {
                    if (handler.autoSave()) {
                        statement.addBatch("INSERT INTO HANDLERS(NAME, TYPE, PRIORITY, ENABLED) VALUES ('" +
                                handler.getName() + "', '" +
                                handler.getUniqueTypeString() + "', " +
                                handler.getPriority() + ", " +
                                (handler.isEnabled() ? "TRUE" : "FALSE") + ");");
                    }
                }
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void markForDeletion(String databaseDir) {
        if (FGConfigManager.getInstance().purgeDatabases()) {
            FoxGuardMain.instance().logger().info("Clearing database " + databaseDir);
            try (Connection conn = FoxGuardMain.instance().getDataSource(databaseDir).getConnection()) {
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
        Server server = Sponge.getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String databaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/regions/";
            } else {
                databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            databaseDir += region.getName();
            markForDeletion(databaseDir);
        } else if (object instanceof IHandler) {
            String databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/handlers/" + object.getName();
            markForDeletion(databaseDir);
        }
    }

    private synchronized void unmarkForDeletion(String databaseDir) {
        try (Connection conn = FoxGuardMain.instance().getDataSource(databaseDir).getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("DROP ALL OBJECTS;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (markedForDeletion.contains(databaseDir)) {
            FoxGuardMain.instance().logger().info("Unmarking " + databaseDir + " for deletion.");
            markedForDeletion.remove(databaseDir);
        }
    }

    public synchronized void unmarkForDeletion(IFGObject object) {
        Server server = Sponge.getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String databaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/regions/";
            } else {
                databaseDir = "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            databaseDir += region.getName();
            unmarkForDeletion(databaseDir);
        } else if (object instanceof IHandler) {
            if (server.getDefaultWorld().isPresent()) {
                unmarkForDeletion("jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/handlers/" + object.getName());
            }
        }
    }

    public synchronized void purgeDatabases() {
        for (String databaseDir : markedForDeletion) {
            FoxGuardMain.instance().logger().info("Deleting database " + databaseDir);
            try (Connection conn = FoxGuardMain.instance().getDataSource(databaseDir).getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    statement.execute("DROP ALL OBJECTS DELETE FILES;");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public synchronized void resolveDeferredObjects() {
        for (DeferredObject o : deferedObjects) {
            try {
                IFGObject result = o.resolve();
                if (result == null)
                    FoxGuardMain.instance().logger().warn("Unable to resolve deferred object:\n" + o.toString());
            } catch (SQLException e) {
                FoxGuardMain.instance().logger().warn("Unable to resolve deferred object:\n" + o.toString());
                e.printStackTrace();
            }
        }
    }

    public synchronized void loadGlobalHandler() {
        Server server = Sponge.getGame().getServer();
        try {
            FGManager.getInstance().getGlobalHandler().loadFromDatabase(FoxGuardMain.instance().getDataSource(
                    "jdbc:h2:" + Sponge.getGame().getSavesDirectory().resolve(server.getDefaultWorldName()) + "/foxguard/handlers/" + GlobalHandler.NAME
            ));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


