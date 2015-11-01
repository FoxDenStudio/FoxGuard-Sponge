package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

    private List<IFGObject> markedAsDirty = new ArrayList<>();
    private List<String> markedForDeletion = new ArrayList<>();

    public static FoxGuardStorageManager getInstance() {
        new FoxGuardStorageManager();
        return instance;
    }

    public void initFlagSets() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS FLAGSETS (" +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256)," +
                            "PRIORITY INTEGER);");

        }

    }

    public void initWorld(World world) throws SQLException {
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
        }
    }

    public void loadFlagSets() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();

        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            ResultSet set = conn.createStatement().executeQuery("SELECT * FROM FLAGSETS;");
            while (set.next()) {
                String databaseDir = "jdbc:h2:./" +
                        server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" +
                        set.getString("NAME");
                DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                try (Connection metaConn = source.getConnection()) {
                    ResultSet metaTables = metaConn.createStatement().executeQuery(
                            "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');");
                    metaTables.first();
                    if (metaTables.getInt(1) != 0) {
                        ResultSet metaSet = metaConn.createStatement().executeQuery("SELECT * FROM FOXGUARD_META.METADATA;");
                        metaSet.first();
                        FoxGuardManager.getInstance().addFlagSet(
                                FGFactoryManager.getInstance().createFlagSet(
                                        source, set.getString("NAME"), set.getString("TYPE"), metaSet.getInt("PRIORITY")));
                    } else {
                        markForDeletion(databaseDir);
                    }
                }
            }
        }
    }

    public void loadWorldRegions(World world) throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String worldDatabaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            worldDatabaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }

        try (Connection conn = FoxGuardMain.getInstance().getDataSource(worldDatabaseDir + "foxguard").getConnection()) {
            ResultSet regionSet = conn.createStatement().executeQuery("SELECT * FROM REGIONS;");
            while (regionSet.next()) {
                String databaseDir = worldDatabaseDir + "regions/" + regionSet.getString("NAME");
                DataSource source = FoxGuardMain.getInstance().getDataSource(databaseDir);
                try (Connection metaConn = source.getConnection()) {
                    ResultSet metaTables = metaConn.createStatement().executeQuery(
                            "SELECT COUNT(*) FROM (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'FOXGUARD_META');");
                    metaTables.first();
                    if (metaTables.getInt(1) != 0) {
                        ResultSet metaSet = metaConn.createStatement().executeQuery("SELECT * FROM FOXGUARD_META.METADATA;");
                        metaSet.first();
                        String type = regionSet.getString("TYPE");
                        FoxGuardManager.getInstance().addRegion(world,
                                FGFactoryManager.getInstance().createRegion(
                                        source,
                                        regionSet.getString("NAME"), type
                                )
                        );
                    } else {
                        markForDeletion(databaseDir);
                    }
                }
            }

        }
    }

    public void loadWorldLinks(World world) throws SQLException {
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
        }

    }

    public void loadLinks() throws SQLException {
        for (World world : FoxGuardMain.getInstance().getGame().getServer().getWorlds()) {
            loadWorldLinks(world);
        }
    }

    public void writeFlagSets() throws SQLException {
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
        }
        for (IFlagSet flagSet : FoxGuardManager.getInstance().getFlagSetsListCopy()) {
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
        }
    }

    public void writeWorld(World world) throws SQLException {
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
        }
        for (IRegion region : FoxGuardManager.getInstance().getRegionsListCopy(world)) {
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
        }
    }

    public void markForDeletion(String databaseDir) {
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

    public void purgeDatabases() throws SQLException {
        for (String databaseDir : markedForDeletion) {
            try (Connection conn = FoxGuardMain.getInstance().getDataSource(databaseDir).getConnection()) {
                conn.createStatement().execute("DROP ALL OBJECTS DELETE FILES;");
            }
        }
    }
}


