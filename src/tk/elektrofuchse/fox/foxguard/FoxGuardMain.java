package tk.elektrofuchse.fox.foxguard;

import com.flowpowered.math.vector.Vector2i;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.living.player.TargetPlayerEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.event.EventManager;
import org.spongepowered.api.service.sql.SqlService;
import tk.elektrofuchse.fox.foxguard.commands.*;
import tk.elektrofuchse.fox.foxguard.flags.SimpleFlagSet;
import tk.elektrofuchse.fox.foxguard.listener.BlockEventListener;
import tk.elektrofuchse.fox.foxguard.listener.PlayerEventListener;
import tk.elektrofuchse.fox.foxguard.listener.RightClickHandler;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;

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

    private FoxGuardCommandDispatcher fgDispatcher;

    @Listener
    public void initFoxGuard(GameInitializationEvent event) {
        instance = this;
        new FoxGuardManager(this, game.getServer());
        FoxGuardManager.getInstance().loadLists();
        eventManager.registerListener(this, TargetPlayerEvent.class, new PlayerEventListener());
        eventManager.registerListener(this, ChangeBlockEvent.class, new BlockEventListener());
        eventManager.registerListener(this, InteractBlockEvent.class, new RightClickHandler());
        registerCommands();

    }

    @Listener
    public void setupWorld(GameStartedServerEvent event) {
        FoxGuardManager fgm = FoxGuardManager.getInstance();
        fgm.setup(game.getServer());
        fgm.addFlagSet(new SimpleFlagSet("test", 1));
        fgm.addRegion(game.getServer().getWorld("world").get(),
                new RectRegion("test", new BoundingBox2(new Vector2i(-100, -100), new Vector2i(100, 100))));
        fgm.link(game.getServer(), "world", "test", "test");
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

    private void registerCommands() {
        fgDispatcher = new FoxGuardCommandDispatcher();
        fgDispatcher.register(new CommandCreate(), "create", "new", "make", "define", "mk");
        fgDispatcher.register(new CommandTest(), "test");
        fgDispatcher.register(new CommandList(), "list", "ls");
        fgDispatcher.register(new CommandPosition(), "position", "pos");
        fgDispatcher.register(new CommandState(), "state", "current", "cur");
        fgDispatcher.register(new CommandFlush(), "flush", "clear");
        fgDispatcher.register(new CommandDetail(), "detail", "show");
        game.getCommandDispatcher().register(this, fgDispatcher, "foxguard", "foxg", "fg");
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

    public static FoxGuardMain getInstance() {
        return instance;
    }
}
