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

import com.google.inject.Inject;
import net.foxdenstudio.sponge.foxcore.plugin.FoxCoreMain;
import net.foxdenstudio.sponge.foxcore.plugin.command.CommandAbout;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandDispatcher;
import net.foxdenstudio.sponge.foxcore.plugin.state.FCStateManager;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.command.*;
import net.foxdenstudio.sponge.foxguard.plugin.controller.LogicController;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagRegistry;
import net.foxdenstudio.sponge.foxguard.plugin.handler.*;
import net.foxdenstudio.sponge.foxguard.plugin.listener.*;
import net.foxdenstudio.sponge.foxguard.plugin.misc.FGContextCalculator;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.CuboidRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.ElevationRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.RectangularRegion;
import net.foxdenstudio.sponge.foxguard.plugin.state.ControllersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.HandlersStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.RegionsStateField;
import net.foxdenstudio.sponge.foxguard.plugin.state.factory.ControllersStateFieldFactory;
import net.foxdenstudio.sponge.foxguard.plugin.state.factory.HandlersStateFieldFactory;
import net.foxdenstudio.sponge.foxguard.plugin.state.factory.RegionsStateFieldFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.*;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Plugin(id = "foxguard",
        name = "FoxGuard",
        dependencies = {
                @Dependency(id = "foxcore")
        },
        description = "A world protection plugin built for SpongeAPI. Requires FoxCore.",
        authors = {"gravityfox", "d4rkfly3r", "vectrix", "Waterpicker"},
        url = "https://github.com/FoxDenStudio/FoxGuard")
public final class FoxGuardMain {

    public final Cause pluginCause = Cause.builder().named("plugin", this).build();

    /**
     * FoxGuardMain instance object.
     */
    private static FoxGuardMain instanceField;

    private Logger logger = LoggerFactory.getLogger("fox.guard");

    @Inject
    private Game game;

    @Inject
    private EventManager eventManager;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path configDirectory;

    @Inject
    private PluginContainer container;

//    @Inject
//    private SpongeStatsLite stats;

    private UserStorageService userStorage;
    private EconomyService economyService = null;

    private boolean loaded = false;

    private boolean statusOK = true;
    private boolean configOK = true;
    private boolean managerOk = true;

    private FCCommandDispatcher fgDispatcher;

    /**
     * @return The current instance of the FoxGuardMain object.
     */
    public static FoxGuardMain instance() {
        return instanceField;
    }

    //my uuid - f275f223-1643-4fac-9fb8-44aaf5b4b371

    @Listener
    public void construct(GameConstructionEvent event) {
        instanceField = this;
    }

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        logger.info("Beginning FoxGuard initialization");
        logger.info("Version: " + container.getVersion().orElse("Unknown"));

        try {
            logger.info("Loading configs");
            FGConfigManager.getInstance();
        } catch (Exception e) {
            logger.error("Error while loading configs! Using defaults.", e);
            configOK = false;
        }
        if (configOK) {
            try {
                logger.info("Saving configs");
                FGConfigManager.getInstance().save();
            } catch (Exception e) {
                logger.error("Error while saving configs", e);
                configOK = false;
            }
        } else {
            logger.warn("Skipping config re-save due to failed config loading");
        }

        try {
            logger.info("Initializing FoxGuard manager instance");
            FGManager.init();
        } catch (Exception e) {
            initializationError("Error initializing FoxGuard manager instance", e);
            managerOk = false;
        }

//        logger.info("Starting MCStats metrics extension");
//        stats.start();
    }

    @Listener
    public void init(GameInitializationEvent event) {
        if (managerOk) {
            logger.info("Registering regions state field");
            FCStateManager.instance().registerStateFactory(new RegionsStateFieldFactory(), RegionsStateField.ID, RegionsStateField.ID, Aliases.REGIONS_ALIASES);
            logger.info("Registering handlers state field");
            FCStateManager.instance().registerStateFactory(new HandlersStateFieldFactory(), HandlersStateField.ID, HandlersStateField.ID, Aliases.HANDLERS_ALIASES);
            logger.info("Registering controllers state field");
            FCStateManager.instance().registerStateFactory(new ControllersStateFieldFactory(), ControllersStateField.ID, ControllersStateField.ID, Aliases.CONTROLLERS_ALIASES);
            logger.info("Registering FoxGuard object factories");
            registerFactories();
        } else {
            logger.warn("Skipping registration of state fields and object factories due to manager initialization failure");
        }

        try {
            logger.info("Getting User Storage");
            userStorage = game.getServiceManager().provide(UserStorageService.class).get();
        } catch (Exception e) {
            initializationError("Error retrieving User Storage Service from Sponge", e);
        }

        try {
            logger.info("Registering event listeners");
            registerEventListeners();
        } catch (Exception e) {
            logger.error("Error registering event Listeners", e);
        }
    }

    @Listener
    public void registerCommands(GameInitializationEvent event) {
        logger.info("Registering commands");
        fgDispatcher = new FCCommandDispatcher("/foxguard",
                "FoxGuard commands for managing world protection. Use /help foxguard for subcommands.");

        fgDispatcher.register(new CommandCreate(), "create", "construct", "new", "make", "define", "mk", "cr");
        fgDispatcher.register(new CommandDelete(), "delete", "del", "remove", "rem", "rm", "destroy");
        fgDispatcher.register(new CommandModify(), "modify", "mod", "change", "edit", "update", "md", "ch");
        fgDispatcher.register(new CommandRename(), "rename", "name", "rn");
        fgDispatcher.register(new CommandLink(), "link", "connect", "attach");
        fgDispatcher.register(new CommandUnlink(), "unlink", "disconnect", "detach");
        fgDispatcher.register(new CommandEnableDisable(true), "enable", "activate", "engage", "on");
        fgDispatcher.register(new CommandEnableDisable(false), "disable", "deactivate", "disengage", "off");
        fgDispatcher.register(new CommandList(), "list", "ls");
        fgDispatcher.register(new CommandHere(), "here", "around");
        fgDispatcher.register(new CommandDetail(), "detail", "det", "show");
        fgDispatcher.register(new CommandSave(), "save", "saveall", "save-all");

        fgDispatcher.register(new CommandPriority(), "priority", "prio", "level", "rank");

        fgDispatcher.register(new CommandTest(), "test");
        fgDispatcher.register(new CommandLink2(true), "link2", "connect2", "attach2");
        fgDispatcher.register(new CommandLink2(false), "unlink2", "disconnect2", "detach2");
        registerCoreCommands(fgDispatcher);

        game.getCommandManager().register(this, fgDispatcher, "foxguard", "foxg", "fguard", "fg");
    }

    @Listener
    public void setupEconomy(GamePostInitializationEvent event) {
        Optional<EconomyService> economyServiceOptional = Sponge.getGame().getServiceManager().provide(EconomyService.class);
        if (economyServiceOptional.isPresent()) {
            economyService = economyServiceOptional.get();
        }
    }

    @Listener
    public void configurePermissions(GamePostInitializationEvent event) {
        logger.info("Configuring permissions");
        PermissionService service = game.getServiceManager().provide(PermissionService.class).get();
        service.getDefaults().getTransientSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "foxguard.override", Tristate.FALSE);
        service.registerContextCalculator(new FGContextCalculator());
    }

    @Listener
    public void serverStarting(GameStartingServerEvent event) {
        if (managerOk) {
            logger.info("Loading regions");
            FGStorageManager.getInstance().loadRegions();
            logger.info("Loading global handler");
            FGStorageManager.getInstance().loadGlobalHandler();
            logger.info("Loading handlers");
            FGStorageManager.getInstance().loadHandlers();
            logger.info("Loading linkages");
            FGStorageManager.getInstance().loadLinks();
            loaded = true;
            logger.info("Finished loading FoxGuard!");
        } else {
            logger.warn("Skipping server protection object loading due to manager initialization failure");
        }
    }

    @Listener
    public void serverStopping(GameStoppingServerEvent event) {
        if (managerOk) {
            FGStorageManager.getInstance().saveRegions();
            FGStorageManager.getInstance().saveHandlers();
            logger.info("Saving configs");
            FGConfigManager.getInstance().save();
        } else {
            logger.warn("Skipping server-stop saving due to manager initialization failure");
        }
    }

    @Listener
    public void worldUnload(UnloadWorldEvent event) {
        if (managerOk) {
            logger.info("Unloading world \"" + event.getTargetWorld().getName() + "\"");
            FGStorageManager.getInstance().saveWorldRegions(event.getTargetWorld());
            FGManager.getInstance().unloadWorld(event.getTargetWorld());
        } else {
            logger.warn("Skipping world-unload saving due to manager initialization failure");
        }
    }

    @Listener
    public void worldLoad(LoadWorldEvent event) {
        if (managerOk) {
            logger.info("Initializing global worldregion for world: \"" + event.getTargetWorld().getName() + "\"");
            FGManager.getInstance().initWorld(event.getTargetWorld());
            logger.info("Loading worldregions for world: \"" + event.getTargetWorld().getName() + "\"");
            FGStorageManager.getInstance().loadWorldRegions(event.getTargetWorld());
            if (loaded) {
                logger.info("Loading links for world : \"" + event.getTargetWorld().getName() + "\"");
                FGStorageManager.getInstance().loadWorldRegionLinks(event.getTargetWorld());
            }
        } else {
            logger.warn("Skipping world object loading due to manager initialization failure.");
        }
    }

    private void registerCoreCommands(FCCommandDispatcher dispatcher) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "FoxGuard World Protection Plugin\n"));
        builder.append(Text.of("Version: " + container.getVersion().orElse("Unknown") + "\n"));
        builder.append(Text.of("Author: gravityfox\n"));

        for (CommandMapping mapping : FoxCoreMain.instance().getFCDispatcher().getCommands()) {
            if (mapping.getCallable() instanceof net.foxdenstudio.sponge.foxcore.plugin.command.CommandTest)
                continue;
            Set<String> secondary = new HashSet<>(mapping.getAllAliases());
            secondary.remove(mapping.getPrimaryAlias());
            CommandCallable callable = mapping.getCallable();
            if (callable instanceof CommandAbout) ((CommandAbout) callable).addText(builder.build());
            dispatcher.register(callable, mapping.getPrimaryAlias(), new ArrayList<>(secondary));
        }
    }

    private void registerFactories() {
        FGFactoryManager manager = FGFactoryManager.getInstance();
        manager.registerWorldRegionFactory(new RectangularRegion.Factory());
        manager.registerWorldRegionFactory(new CuboidRegion.Factory());
        manager.registerWorldRegionFactory(new ElevationRegion.Factory());

        manager.registerHandlerFactory(new StaticHandler.Factory());
        manager.registerHandlerFactory(new BasicHandler.Factory());
        manager.registerHandlerFactory(new GroupHandler.Factory());
        manager.registerHandlerFactory(new PermissionHandler.Factory());
        manager.registerHandlerFactory(new DebugHandler.Factory());

        //manager.registerControllerFactory(new MessageController.Factory());
        manager.registerControllerFactory(new LogicController.Factory());
    }


    /**
     * A private method that registers the Listener class and the corresponding event class.
     */
    private void registerEventListeners() {
        registerListeners(this, FlagRegistry.getInstance());
        registerListener(this, ChangeBlockEvent.class, Order.LATE, new BlockChangeListener());
        registerListener(this, InteractBlockEvent.class, Order.LATE, new InteractBlockListener());
        registerListener(this, InteractEntityEvent.class, Order.LATE, new InteractEntityListener());
        registerListener(this, SpawnEntityEvent.class, Order.LATE, new SpawnEntityListener());
        if (FGConfigManager.getInstance().getModules().get(FGConfigManager.Module.MOVEMENT)) {
            PlayerMoveListener pml = new PlayerMoveListener(true);
            registerListener(this, MoveEntityEvent.class, pml);
            registerListeners(this, pml.new Listeners());
        }
        registerListener(this, ExplosionEvent.class, Order.LATE, new ExplosionListener());
        registerListener(this, DamageEntityEvent.class, Order.LATE, new DamageListener());
    }

    public void registerListeners(Object plugin, Object obj) {
        try {
            eventManager.registerListeners(plugin, obj);
        } catch (Exception e) {
            logger.error("Failed to register listener: " + obj.toString());
            logger.error("Of class: " + obj.getClass());
            logger.error("This Listener will not respond to events", e);
        }
    }

    public <T extends Event> void registerListener(Object plugin, Class<T> eventClass, EventListener<? super T> listener) {
        try {
            eventManager.registerListener(plugin, eventClass, listener);
        } catch (Exception e) {
            logger.error("Failed to register listener: " + listener.toString());
            logger.error("With the event class: " + eventClass.getName());
            logger.error("This Listener will not respond to events", e);
        }
    }

    public <T extends Event> void registerListener(Object plugin, Class<T> eventClass, Order order, EventListener<? super T> listener) {
        try {
            eventManager.registerListener(plugin, eventClass, order, listener);
        } catch (Exception e) {
            logger.error("Failed to register listener: " + listener.toString());
            logger.error("With the event class: " + eventClass.getName());
            logger.error("This Listener will not respond to events", e);
        }
    }

    private void initializationError(@Nullable String message, @Nullable Throwable throwable) {
        this.statusOK = false;
        logger.error("-------------------------------------------------------------------------------------------");
        logger.error("------------------------------ FOXGUARD INITIALIZATION ERROR ------------------------------");
        logger.error("-------------------------------------------------------------------------------------------");
        logger.error("");
        if (throwable == null && (message == null || message.isEmpty())) {
            logger.error("FoxGuard has encountered an unknown error while trying to initialize!");
        } else {
            logger.error("FoxGuard has encountered an error while trying to initialize!");
            logger.error("");

            if (message != null && !message.isEmpty()) {
                logger.error("Message: " + message);
                logger.error("");

            }
            if (throwable != null) {
                logger.error("Stacktrace:", throwable);
                logger.error("");

            }
        }
        logger.error("-------------------------------------------------------------------------------------------");
        logger.error("");
        logger.error("FOXGUARD WILL NOT BE ABLE TO INITIALIZE COMPLETELY FROM THIS POINT ONWARD!");
        logger.error("ANY ERRORS OR EXCEPTIONS BY FOXGUARD AFTER THIS POINT ARE PROBABLY BECAUSE OF THIS ERROR!");
        logger.error("WHEN REPORTING BUGS, PLEASE REPORT THESE ERRORS FIRST, AS THEY ARE THE MOST IMPORTANT!");
        logger.error("");
        logger.error("-------------------------------------------------------------------------------------------");
        logger.error("-------------------------------------------------------------------------------------------");
        logger.error("-------------------------------------------------------------------------------------------");

    }

    /**
     * @return A Logger instance for this plugin.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Method that when called will return a UserStorageService object that can be used to store or retrieve data corresponding to a specific player.
     *
     * @return UserStorageService object.
     */
    public UserStorageService getUserStorage() {
        if (userStorage == null) {
            logger.info("Getting User Storage");
            userStorage = game.getServiceManager().provide(UserStorageService.class).get();
        }
        return userStorage;
    }

    /**
     * @return A File object corresponding to the config of the plugin.
     */
    public Path getConfigDirectory() {
        return configDirectory;
    }

    /**
     * Whether or not server (not world specific) data has been loaded yet.
     * Is true if the {@code GameStartingServerEvent} has fired, and the server data has been loaded.
     * Is primarily used to indicate to post-start world loading code to do handler linking.
     *
     * @return Whether server data has been loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    public static Cause getCause() {
        return instance().pluginCause;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }
}
