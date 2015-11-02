/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.util.DeferredObject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 10/26/2015.
 * Project: foxguard
 */
public class FoxGuardStorageManager {
    private static FoxGuardStorageManager instance;

    private FoxGuardStorageManager() {
        if (instance == null) instance = this;
    }

    private List<String> markedForDeletion = new ArrayList<>();

    private List<DeferredObject> deferedObjects = new LinkedList<>();

    public static FoxGuardStorageManager getInstance() {
        new FoxGuardStorageManager();
        return instance;
    }

    public void initFlagSets() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS FLAGSETS (" +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256)," +
                            "PRIORITY INTEGER);");

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void initWorld(World world) {
        String dataBaseDir;
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS REGIONS (" +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256));" +
                            "CREATE TABLE IF NOT EXISTS LINKAGES (" +
                            "REGION VARCHAR(256)," +
                            "FLAGSET VARCHAR(256));");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadFlagSets() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();

        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            ResultSet flagSetDataSet = conn.createStatement().executeQuery("SELECT * FROM FLAGSETS;");
            while (flagSetDataSet.next()) {
                try {
                    String databaseDir = "jdbc:h2:./" +
                            server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" +
                            flagSetDataSet.getString("NAME");
                    DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                    try (Connection metaConn = source.getConnection()) {
                        ResultSet metaTables = metaConn.createStatement().executeQuery(
                                "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');");
                        metaTables.first();
                        if (metaTables.getInt(1) != 0) {
                            ResultSet metaSet = metaConn.createStatement().executeQuery("SELECT * FROM FOXGUARD_META.METADATA;");
                            metaSet.first();
                            if (metaSet.getString("CATEGORY").equalsIgnoreCase("flagset") &&
                                    metaSet.getString("NAME").equalsIgnoreCase(flagSetDataSet.getString("NAME")) &&
                                    metaSet.getString("TYPE").equalsIgnoreCase(flagSetDataSet.getString("TYPE")) &&
                                    metaSet.getInt("PRIORITY") == flagSetDataSet.getInt("PRIORITY")) {
                                FoxGuardManager.getInstance().addFlagSet(
                                        FGFactoryManager.getInstance().createFlagSet(
                                                source, flagSetDataSet.getString("NAME"), flagSetDataSet.getString("TYPE"), flagSetDataSet.getInt("PRIORITY")));
                            } else if (FGConfigManager.getInstance().forceLoad) {
                                FoxGuardMain.getInstance().getLogger().info("Mismatched database found. Attempting force load...");
                                if (metaSet.getString("CATEGORY").equalsIgnoreCase("region")) {
                                    DeferredObject deferredRegion = new DeferredObject();
                                    deferredRegion.category = "region";
                                    deferredRegion.dataSource = source;
                                    deferredRegion.databaseDir = databaseDir;
                                    deferredRegion.listName = flagSetDataSet.getString("NAME");
                                    deferredRegion.metaName = metaSet.getString("NAME");
                                    deferredRegion.type = metaSet.getString("TYPE");
                                    deferredRegion.listWorld = null;
                                    deferredRegion.metaWorld = metaSet.getString("WORLD");
                                    this.deferedObjects.add(deferredRegion);
                                } else if (metaSet.getString("CATEGORY").equalsIgnoreCase("flagset")) {
                                    DeferredObject deferredFlagSet = new DeferredObject();
                                    deferredFlagSet.category = "flagset";
                                    deferredFlagSet.dataSource = source;
                                    deferredFlagSet.databaseDir = databaseDir;
                                    deferredFlagSet.listName = flagSetDataSet.getString("NAME");
                                    deferredFlagSet.metaName = metaSet.getString("NAME");
                                    deferredFlagSet.type = metaSet.getString("TYPE");
                                    deferredFlagSet.priority = metaSet.getInt("PRIORITY");
                                    this.deferedObjects.add(deferredFlagSet);
                                } else {
                                    FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                    markForDeletion(databaseDir);
                                }
                            } else {
                                FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                markForDeletion(databaseDir);
                            }
                        } else {
                            FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                            markForDeletion(databaseDir);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadWorldRegions(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String worldDatabaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.getInstance().getDataSource(worldDatabaseDir + "foxguard").getConnection()) {
            ResultSet regionDataSet = conn.createStatement().executeQuery("SELECT * FROM REGIONS;");
            while (regionDataSet.next()) {
                String databaseDir;
                try {
                    databaseDir = worldDatabaseDir + "regions/" + regionDataSet.getString("NAME");
                    DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                    try (Connection metaConn = source.getConnection()) {
                        ResultSet metaTables = metaConn.createStatement().executeQuery(
                                "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');");
                        metaTables.first();
                        if (metaTables.getInt(1) != 0) {
                            ResultSet metaSet = metaConn.createStatement().executeQuery("SELECT * FROM FOXGUARD_META.METADATA;");
                            metaSet.first();
                            if (metaSet.getString("CATEGORY").equalsIgnoreCase("region") &&
                                    metaSet.getString("NAME").equalsIgnoreCase(regionDataSet.getString("NAME")) &&
                                    metaSet.getString("TYPE").equalsIgnoreCase(regionDataSet.getString("TYPE")) &&
                                    metaSet.getString("WORLD").equals(world.getName())) {
                                FoxGuardManager.getInstance().addRegion(world,
                                        FGFactoryManager.getInstance().createRegion(
                                                source,
                                                regionDataSet.getString("NAME"), regionDataSet.getString("TYPE")
                                        )
                                );
                            } else if (FGConfigManager.getInstance().forceLoad) {
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
                                    this.deferedObjects.add(deferredRegion);
                                } else if (metaSet.getString("CATEGORY").equalsIgnoreCase("flagset")) {
                                    DeferredObject deferredFlagSet = new DeferredObject();
                                    deferredFlagSet.category = "flagset";
                                    deferredFlagSet.dataSource = source;
                                    deferredFlagSet.databaseDir = databaseDir;
                                    deferredFlagSet.listName = regionDataSet.getString("NAME");
                                    deferredFlagSet.metaName = metaSet.getString("NAME");
                                    deferredFlagSet.type = metaSet.getString("TYPE");
                                    deferredFlagSet.priority = metaSet.getInt("PRIORITY");
                                    this.deferedObjects.add(deferredFlagSet);
                                } else {
                                    FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                    markForDeletion(databaseDir);
                                }
                            } else {
                                FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                                markForDeletion(databaseDir);
                            }
                        } else {
                            FoxGuardMain.getInstance().getLogger().info("Found potentially corrupted database.");
                            markForDeletion(databaseDir);
                        }
                    }
                } catch (SQLException e) {
                    FoxGuardMain.getInstance().getLogger().info("Unable to load Region in world \"" + world.getName() + "\". Perhaps the database is corrupted?");
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadWorldLinks(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            ResultSet linkSet = conn.createStatement().executeQuery("SELECT  * FROM LINKAGES;");
            while (linkSet.next()) {
                FoxGuardManager.getInstance().link(world, linkSet.getString("REGION"), linkSet.getString("FLAGSET"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void loadLinks() {
        FoxGuardMain.getInstance().getGame().getServer().getWorlds().forEach(this::loadWorldLinks);
    }

    public void writeFlagSets() {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            statement.addBatch("DELETE FROM FLAGSETS;");
            for (IFlagSet flagSet : FoxGuardManager.getInstance().getFlagSetsListCopy()) {
                statement.addBatch("INSERT INTO FLAGSETS(NAME, TYPE, PRIORITY) VALUES ('" +
                        flagSet.getName() + "', '" +
                        flagSet.getUniqueType() + "', " +
                        flagSet.getPriority() + ");");
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (IFlagSet flagSet : FoxGuardManager.getInstance().getFlagSetsListCopy()) {
            try {
                DataSource source = FoxGuardMain.getInstance().getDataSource(
                        "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() +
                                "/foxguard/flagsets/" + flagSet.getName());
                flagSet.writeToDatabase(source);
                try (Connection conn = source.getConnection()) {
                    Statement statement = conn.createStatement();
                    statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                    statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                            "CATEGORY VARCHAR(16), " +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256), " +
                            "PRIORITY INTEGER);");
                    statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                    statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, PRIORITY) VALUES (" +
                            "'flagset', '" +
                            flagSet.getName() + "', '" +
                            flagSet.getUniqueType() + "', " +
                            flagSet.getPriority() + ");");
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeWorld(World world) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            statement.addBatch("DELETE FROM REGIONS; DELETE FROM LINKAGES;");
            for (IRegion region : FoxGuardManager.getInstance().getRegionsListCopy(world)) {
                statement.addBatch("INSERT INTO REGIONS(NAME, TYPE) VALUES ('" +
                        region.getName() + "', '" +
                        region.getUniqueType() + "');");
                for (IFlagSet flagSet : region.getFlagSets()) {
                    statement.addBatch("INSERT INTO LINKAGES(REGION, FLAGSET) VALUES ('" +
                            region.getName() + "', '" +
                            flagSet.getName() + "');");
                }
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (IRegion region : FoxGuardManager.getInstance().getRegionsListCopy(world)) {
            try {
                DataSource source = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "regions/" + region.getName());
                region.writeToDatabase(source);
                try (Connection conn = source.getConnection()) {
                    Statement statement = conn.createStatement();
                    statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                    statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                            "CATEGORY VARCHAR(16), " +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256), " +
                            "WORLD VARCHAR(256));");
                    statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                    statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, WORLD) VALUES (" +
                            "'region', '" +
                            region.getName() + "', '" +
                            region.getUniqueType() + "', '" +
                            region.getWorld().getName() + "');");
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void update(IFGObject object) {
        if (object instanceof IRegion)
            updateRegion((IRegion) object);
        else if (object instanceof IFlagSet)
            updateFlagSet((IFlagSet) object);
    }

    public void updateRegion(IRegion region) {
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
                Statement statement = conn.createStatement();
                statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                        "CATEGORY VARCHAR(16), " +
                        "NAME VARCHAR(256), " +
                        "TYPE VARCHAR(256), " +
                        "WORLD VARCHAR(256));");
                statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, WORLD) VALUES (" +
                        "'region', '" +
                        region.getName() + "', '" +
                        region.getUniqueType() + "', '" +
                        region.getWorld().getName() + "');");
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFlagSet(IFlagSet flagSet) {
        if(!FoxGuardMain.getInstance().isLoaded()) return;
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try {
            DataSource source = FoxGuardMain.getInstance().getDataSource(
                    "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() +
                            "/foxguard/flagsets/" + flagSet.getName());
            flagSet.writeToDatabase(source);
            try (Connection conn = source.getConnection()) {
                Statement statement = conn.createStatement();
                statement.addBatch("CREATE SCHEMA IF NOT EXISTS FOXGUARD_META;");
                statement.addBatch("CREATE TABLE IF NOT EXISTS FOXGUARD_META.METADATA(" +
                        "CATEGORY VARCHAR(16), " +
                        "NAME VARCHAR(256), " +
                        "TYPE VARCHAR(256), " +
                        "PRIORITY INTEGER);");
                statement.addBatch("DELETE FROM FOXGUARD_META.METADATA");
                statement.addBatch("INSERT INTO FOXGUARD_META.METADATA(CATEGORY, NAME, TYPE, PRIORITY) VALUES (" +
                        "'flagset', '" +
                        flagSet.getName() + "', '" +
                        flagSet.getUniqueType() + "', " +
                        flagSet.getPriority() + ");");
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markForDeletion(String databaseDir) {
        if (FGConfigManager.getInstance().purgeDatabases) {
            try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
                conn.createStatement().execute("DROP ALL OBJECTS;");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (!markedForDeletion.contains(databaseDir)) markedForDeletion.add(databaseDir);
    }

    public void markForDeletion(IFGObject object) {
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
        } else if (object instanceof IFlagSet) {
            String databaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" + object.getName();
            markForDeletion(databaseDir);
        }
    }

    public void unmarkForDeletion(String databaseDir) {
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
            conn.createStatement().execute("DROP ALL OBJECTS;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        markedForDeletion.remove(databaseDir);
    }

    public void unmarkForDeletion(IFGObject object) {
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
        } else if (object instanceof IFlagSet) {
            if (server.getDefaultWorld().isPresent()) {
                unmarkForDeletion("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" + object.getName());
            }
        }
    }

    public void purgeDatabases() {
        if (FGConfigManager.getInstance().purgeDatabases) {
            for (String databaseDir : markedForDeletion) {
                try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
                    conn.createStatement().execute("DROP ALL OBJECTS DELETE FILES;");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void resolveDeferredObjects() {
        for (DeferredObject o : deferedObjects) {
            try {
                o.resolve();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


