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
import net.foxdenstudio.sponge.foxguard.plugin.handler.BasicHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.DebugHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.GroupHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.StaticHandler;
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
import net.minecrell.mcstats.SpongeStatsLite;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
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
        authors = {"gravityfox"},
        url = "https://github.com/FoxDenStudio/FoxGuard")
public final class FoxGuardMain {

    public final Cause pluginCause = Cause.builder().named("plugin", this).build();

    /**
     * FoxGuardMain instance object.
     */
    private static FoxGuardMain instanceField;

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    private EventManager eventManager;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path configDirectory;

    @Inject
    private PluginContainer container;

    @Inject
    private SpongeStatsLite stats;

    private UserStorageService userStorage;
    private EconomyService economyService = null;

    private boolean loaded = false;

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
        logger.info("Loading configs");
        new FGConfigManager();
        logger.info("Saving configs");
        FGConfigManager.getInstance().save();

        logger.info("Initializing FoxGuard manager instance");
        FGManager.init();

        logger.info("Starting MCStats metrics extension");
        stats.start();
    }

    @Listener
    public void init(GameInitializationEvent event) {
        logger.info("Registering regions state field");
        FCStateManager.instance().registerStateFactory(new RegionsStateFieldFactory(), RegionsStateField.ID, RegionsStateField.ID, Aliases.REGIONS_ALIASES);
        logger.info("Registering handlers state field");
        FCStateManager.instance().registerStateFactory(new HandlersStateFieldFactory(), HandlersStateField.ID, HandlersStateField.ID, Aliases.HANDLERS_ALIASES);
        logger.info("Registering controllers state field");
        FCStateManager.instance().registerStateFactory(new ControllersStateFieldFactory(), ControllersStateField.ID, ControllersStateField.ID, Aliases.CONTROLLERS_ALIASES);
        logger.info("Registering FoxGuard object factories");
        registerFactories();
        logger.info("Getting User Storage");
        userStorage = game.getServiceManager().provide(UserStorageService.class).get();
        logger.info("Registering event listeners");
        registerListeners();
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
    }

    @Listener
    public void serverStopping(GameStoppingServerEvent event) {
        FGStorageManager.getInstance().saveRegions();
        FGStorageManager.getInstance().saveHandlers();
        logger.info("Saving configs");
        FGConfigManager.getInstance().save();
    }

    @Listener
    public void worldUnload(UnloadWorldEvent event) {
        logger.info("Unloading world \"" + event.getTargetWorld().getName() + "\"");
        FGStorageManager.getInstance().saveWorldRegions(event.getTargetWorld());
        FGManager.getInstance().unloadWorld(event.getTargetWorld());
    }

    @Listener
    public void worldLoad(LoadWorldEvent event) {
        logger.info("Initializing global worldregion for world: \"" + event.getTargetWorld().getName() + "\"");
        FGManager.getInstance().initWorld(event.getTargetWorld());
        logger.info("Loading worldregions for world: \"" + event.getTargetWorld().getName() + "\"");
        FGStorageManager.getInstance().loadWorldRegions(event.getTargetWorld());
        if (loaded) {
            logger.info("Loading links for world : \"" + event.getTargetWorld().getName() + "\"");
            FGStorageManager.getInstance().loadWorldRegionLinks(event.getTargetWorld());
        }
    }

    private void registerCoreCommands(FCCommandDispatcher dispatcher) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GOLD, "FoxGuard World Protection Plugin\n"));
        builder.append(Text.of("Version: " + container.getVersion().orElse("Unknown") + "\n"));
        builder.append(Text.of("Author: gravityfox\n"));

        for (CommandMapping mapping : FoxCoreMain.instance().getFCDispatcher().getCommands()) {
            if (mapping.getCallable() instanceof net.foxdenstudio.sponge.foxcore.plugin.command.CommandTest) continue;
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
        manager.registerHandlerFactory(new DebugHandler.Factory());

        //manager.registerControllerFactory(new MessageController.Factory());
        manager.registerControllerFactory(new LogicController.Factory());
    }


    /**
     * A private method that registers the Listener class and the corresponding event class.
     */
    private void registerListeners() {
        eventManager.registerListeners(this, FlagRegistry.getInstance());
        eventManager.registerListener(this, ChangeBlockEvent.class, Order.LATE, new BlockChangeListener());
        eventManager.registerListener(this, InteractBlockEvent.class, Order.LATE, new InteractBlockListener());
        eventManager.registerListener(this, InteractEntityEvent.class, Order.LATE, new InteractEntityListener());
        eventManager.registerListener(this, SpawnEntityEvent.class, Order.LATE, new SpawnEntityListener());
        if (FGConfigManager.getInstance().getModules().get(FGConfigManager.Module.MOVEMENT)) {
            PlayerMoveListener pml = new PlayerMoveListener(true);
            eventManager.registerListener(this, MoveEntityEvent.class, pml);
            eventManager.registerListeners(this, pml.new Listeners());
        }
        eventManager.registerListener(this, ExplosionEvent.class, Order.LATE, new ExplosionListener());
        eventManager.registerListener(this, DamageEntityEvent.class, Order.LATE, new DamageListener());
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
     * Will return true or false depending on if the plugin has loaded properly or not.
     *
     * @return Depending on the loaded variable
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
