/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.util;

import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.FoxGuardStorageManager;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by Fox on 11/1/2015.
 * Project: foxguard
 */
public class DeferredObject {

    public DataSource dataSource;
    public String databaseDir;
    public String category;
    public String type;
    public String listName;
    public String metaName;
    public World listWorld = null;
    public String metaWorld;
    public int priority;

    public void resolve() throws SQLException {
        if (category.equalsIgnoreCase("flagset")) {
            String name;
            if (FoxGuardManager.getInstance().getFlagSet(metaName) == null)
                name = metaName;
            else if (FoxGuardManager.getInstance().getFlagSet(listName) == null)
                name = listName;
            else {
                int x = 1;
                while (FoxGuardManager.getInstance().getFlagSet(metaName + x) != null) {
                    x++;
                }
                name = metaName + x;
            }
            IFlagSet flagSet = FGFactoryManager.getInstance().createFlagSet(dataSource, name, type, priority);
            FoxGuardStorageManager.getInstance().markForDeletion(databaseDir);
            FoxGuardManager.getInstance().addFlagSet(flagSet);
            FoxGuardMain.getInstance().getLogger().info("Successfully force loaded FlagSet: " +
                    "(Name: " + name +
                    " Type: " + type +
                    " Priority: " + priority +
                    ")");
        } else if (category.equalsIgnoreCase("region")) {
            World world = listWorld;
            Optional<World> optWorld = FoxGuardMain.getInstance().getGame().getServer().getWorld(metaWorld);
            if (optWorld.isPresent())
                world = optWorld.get();
            if (world == null) return;
            String name;
            if (FoxGuardManager.getInstance().getRegion(world, metaName) == null)
                name = metaName;
            else if (FoxGuardManager.getInstance().getRegion(world, listName) == null)
                name = listName;
            else {
                int x = 1;
                while (FoxGuardManager.getInstance().getRegion(world, metaName + x) != null) {
                    x++;
                }
                name = metaName + x;
            }
            IRegion region = FGFactoryManager.getInstance().createRegion(dataSource, name, type);
            FoxGuardStorageManager.getInstance().markForDeletion(databaseDir);
            FoxGuardManager.getInstance().addRegion(world, region);
            FoxGuardMain.getInstance().getLogger().info("Successfully force loaded Region: " +
                    "(Name: " + name +
                    " Type: " + type +
                    " World: " + world.getName() +
                    ")");
        }
    }

}
