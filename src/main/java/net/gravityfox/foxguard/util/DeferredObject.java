/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.util;

import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FGStorageManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.IFGObject;
import net.gravityfox.foxguard.factory.FGFactoryManager;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.regions.IRegion;
import org.spongepowered.api.world.World;

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
    public String metaWorld = null;
    public int priority = 0;

    public IFGObject resolve() throws SQLException {
        if (category.equalsIgnoreCase("flagset")) {
            String name;
            if (FGManager.getInstance().getFlagSet(metaName) == null)
                name = metaName;
            else if (FGManager.getInstance().getFlagSet(listName) == null)
                name = listName;
            else {
                int x = 1;
                while (FGManager.getInstance().getFlagSet(metaName + x) != null) {
                    x++;
                }
                name = metaName + x;
            }
            IFlagSet flagSet = FGFactoryManager.getInstance().createFlagSet(dataSource, name, type, priority);
            FGStorageManager.getInstance().markForDeletion(databaseDir);
            FGManager.getInstance().addFlagSet(flagSet);
            FoxGuardMain.getInstance().getLogger().info("Successfully force loaded FlagSet: " +
                    "(Name: " + name +
                    " Type: " + type +
                    " Priority: " + priority +
                    ")");
            return flagSet;
        } else if (category.equalsIgnoreCase("region")) {
            World world = listWorld;
            Optional<World> optWorld = FoxGuardMain.getInstance().getGame().getServer().getWorld(metaWorld);
            if (optWorld.isPresent())
                world = optWorld.get();
            if (world == null) return null;
            String name;
            if (FGManager.getInstance().getRegion(world, metaName) == null)
                name = metaName;
            else if (FGManager.getInstance().getRegion(world, listName) == null)
                name = listName;
            else {
                int x = 1;
                while (FGManager.getInstance().getRegion(world, metaName + x) != null) {
                    x++;
                }
                name = metaName + x;
            }
            IRegion region = FGFactoryManager.getInstance().createRegion(dataSource, name, type);
            FGStorageManager.getInstance().markForDeletion(databaseDir);
            FGManager.getInstance().addRegion(world, region);
            FoxGuardMain.getInstance().getLogger().info("Successfully force loaded Region: " +
                    "(Name: " + name +
                    " Type: " + type +
                    " World: " + world.getName() +
                    ")");
            return region;
        } else return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Database: ").append(databaseDir).append("\n");
        builder.append("Category: ").append(category).append("\n");
        builder.append("Type: ").append(type).append("\n");
        builder.append("Listed name: ").append(listName).append("\n");
        builder.append("Metadata name: ").append(metaName).append("\n");
        if (listWorld != null) builder.append("Listed world: ").append(listWorld.getName()).append("\n");
        if (metaWorld != null) builder.append("Metadata world: ").append(metaWorld).append("\n");
        if (!category.equalsIgnoreCase("region")) builder.append("Priority: ").append(priority).append("\n");
        return builder.toString();
    }
}
