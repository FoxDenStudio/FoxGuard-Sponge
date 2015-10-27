package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.world.World;

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


    // Later on
    public void myMethodThatQueries() throws SQLException {
        try (Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./world/foxguard/regions").getConnection()) {

            conn.prepareStatement("SELECT * FROM test_tbl");

        }

    }

    public void writeRegions() throws SQLException {
        for (World world : FoxGuardMain.getInstance().getGame().getServer().getWorlds()) {
            String dataBaseName;
            if (world.getName().equals("world")) {
                dataBaseName = "jdbc:h2:./world/foxguard/regions";
            } else {
                dataBaseName = "jdbc:h2:./world/" + world.getName() + "/foxguard/regions";
            }
            try (Connection conn = FoxGuardMain.getInstance().getDataSource(dataBaseName).getConnection()) {

            }
        }
    }

    public void init() throws SQLException{
        try(Connection conn = FoxGuardMain.getInstance().getDataSource("jdbc:h2:./world/foxguard/foxguard").getConnection()){
            Statement statement = conn.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS RegionTypes (regionType VARCHAR(64))");
        }
    }
}


