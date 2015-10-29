package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
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
            Statement statement = conn.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS FLAGSETS (" +
                            "NAME VARCHAR(256), " +
                            "TYPE VARCHAR(256));");

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
            Statement statement = conn.createStatement();
            statement.executeUpdate(
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
        ResultSet set;
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            set = statement.executeQuery("SELECT * FROM FLAGSETS;");
        }
        while (set.next()) {
            FoxGuardManager.getInstance().addFlagSet(
                    FGFactoryManager.getInstance().createFlagSet(
                            FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" +
                                            server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" +
                                            set.getString("NAME")
                            ),
                            set.getString("NAME"), set.getString("TYPE")
                    )
            );
        }

    }

    public void loadWorld(World world) throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        String dataBaseDir;
        if (world.getProperties().equals(server.getDefaultWorld().get())) {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/";
        } else {
            dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + world.getName() + "/foxguard/";
        }
        ResultSet regionSet;
        ResultSet linkSet;
        try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseDir + "foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            regionSet = statement.executeQuery("SELECT * FROM REGIONS;");
            linkSet = statement.executeQuery("SELECT  * FROM LINKAGES;");
        }
        while (regionSet.next()) {
            FoxGuardManager.getInstance().addRegion(world,
                    FGFactoryManager.getInstance().createRegion(
                            FoxGuardMain.getInstance().getDataSource(dataBaseDir + "regions/" + regionSet.getString("NAME")),
                            regionSet.getString("NAME"), regionSet.getString("TYPE")
                    )
            );
        }
        while (regionSet.next()) {
            FoxGuardManager.getInstance().link(world, linkSet.getString("REGION"), linkSet.getString("FLAGSET"));
        }
    }

    public void writeFlagSets() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            statement.addBatch("DELETE FROM FLAGSETS;");
            for (IFlagSet flagSet : FoxGuardManager.getInstance().getFlagSetsListCopy()) {
                statement.addBatch("INSERT INTO FLAGSETS(NAME, TYPE) VALUES ('" +
                        flagSet.getName() + "', '" +
                        flagSet.getUniqueType() + "');");
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
                        flagSet.getPriority()+");");
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

    public void markForDeletion(IFGObject object) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String dataBaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/regions/";
            } else {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            dataBaseDir += region.getName();
            if (!markedForDeletion.contains(dataBaseDir)) markedForDeletion.add(dataBaseDir);
        } else if (object instanceof IFlagSet) {
            String dataBaseDir = "jdvx:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" + object.getName();
            if (!markedForDeletion.contains(dataBaseDir)) markedForDeletion.add(dataBaseDir);
        }
    }

    public void unmarkForDeletion(IFGObject object) {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            String dataBaseDir;
            if (region.getWorld().getProperties().equals(server.getDefaultWorld().get())) {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/regions/";
            } else {
                dataBaseDir = "jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/" + region.getWorld().getName() + "/foxguard/regions/";
            }
            dataBaseDir += region.getName();
            markedForDeletion.remove(dataBaseDir);
        } else if (object instanceof IFlagSet) {
            if(server.getDefaultWorld().isPresent()){
            String dataBaseDir = "jdvx:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/flagsets/" + object.getName();
            markedForDeletion.remove(dataBaseDir);}
        }
    }
}


