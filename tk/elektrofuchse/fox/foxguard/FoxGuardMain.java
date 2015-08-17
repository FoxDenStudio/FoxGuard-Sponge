package tk.elektrofuchse.fox.foxguard;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.EventHandler;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.block.BlockChangeEvent;
import org.spongepowered.api.event.block.BlockEvent;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.event.EventManager;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.listener.BlockEventHandler;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Fox on 8/16/2015.
 */
@Plugin(id = "foxguard", name = "FoxGuard", version = "1.0")
public class FoxGuardMain {

    private static FoxGuardMain instance;

    @Inject
    private Logger logger;
    @Inject
    private Game game;
    @Inject
    private EventManager eventManager;
    @Inject
    @ConfigDir(sharedRoot = false)
    File configDirectory;

    private SqlService sql;

    @Subscribe
    public void initFoxGuard(InitializationEvent event){
        instance = this;
        new FoxGuardManager(this);
        FoxGuardManager.getInstance().loadLists();
        eventManager.register(this, BlockChangeEvent.class, new BlockEventHandler());

    }

    @Subscribe
    public void setupWorld(ServerStartedEvent event){
        FoxGuardManager fgm = FoxGuardManager.getInstance();
        event.getGame().getServer().getWorlds().stream()
                .filter(world -> fgm.regions.get(world) == null || fgm.flagSets.get(world) == null)
                .forEach(fgm::populateWorld);
    }







    public javax.sql.DataSource getDataSource(String jdbcUrl) throws SQLException {
        if (sql == null) {
            sql = game.getServiceManager().provide(SqlService.class).get();
        }
        return sql.getDataSource(jdbcUrl);
    }

    // Later on
    public void myMethodThatQueries() throws SQLException {
        try (Connection conn = getDataSource("jdbc:h2:./foxguard/foxtest.db").getConnection()) {
            conn.prepareStatement("SELECT * FROM test_tbl").execute();
        }

    }

    public Logger getLogger() {
        return logger;
    }

    public static FoxGuardMain getInstance() {
        return instance;
    }
}
