package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Fox on 10/26/2015.
 */
public class FoxGuardStorageManager {
    private static FoxGuardStorageManager instance;

    private FoxGuardStorageManager() {
        if (instance == null) instance = this;
    }

    public static FoxGuardStorageManager getInstance() {
        new FoxGuardStorageManager();
        return instance;
    }

    public void writeRegions() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        for (World world : FoxGuardMain.getInstance().getGame().getServer().getWorlds()) {
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
            for(IRegion region : FoxGuardManager.getInstance().getRegionsListCopy(world)){
                
            }
        }
    }

    public void writeFlagSets() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./" + server.getDefaultWorld().get().getWorldName() + "/foxguard/foxguard").getConnection()) {
            Statement statement = conn.createStatement();
            statement.addBatch("DELETE FROM FLAGSETS;");
            for(IFlagSet flagSet : FoxGuardManager.getInstance().getFlagSetsListCopy()){
                statement.addBatch("INSERT INTO FLAGSETS(NAME, TYPE) VALUES ('" +
                        flagSet.getName() + "', '" +
                        flagSet.getUniqueType() + "');");
            }
            statement.executeBatch();
        }
    }

    public void init() throws SQLException {
        Server server = FoxGuardMain.getInstance().getGame().getServer();
        for (World world : FoxGuardMain.getInstance().getGame().getServer().getWorlds()) {
            initWorld(world);
        }
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
}


