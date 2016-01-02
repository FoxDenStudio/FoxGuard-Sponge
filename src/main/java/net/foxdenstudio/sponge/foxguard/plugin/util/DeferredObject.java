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

package net.foxdenstudio.sponge.foxguard.plugin.util;

import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FGStorageManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.FGFactoryManager;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

public class DeferredObject {

    public DataSource dataSource;
    public String databaseDir;
    public String category;
    public String type;
    public String listName;
    public String metaName;
    public boolean listEnabled = true;
    public boolean metaEnabled = true;
    public World listWorld = null;
    public String metaWorld = null;
    public int priority = 0;

    public IFGObject resolve() throws SQLException {
        if (category.equalsIgnoreCase("handler")) {
            String name;
            if (FGManager.getInstance().gethandler(metaName) == null)
                name = metaName;
            else if (FGManager.getInstance().gethandler(listName) == null)
                name = listName;
            else {
                int x = 1;
                while (FGManager.getInstance().gethandler(metaName + x) != null) {
                    x++;
                }
                name = metaName + x;
            }
            IHandler handler = FGFactoryManager.getInstance().createHandler(dataSource, name, type, priority, metaEnabled);
            FGStorageManager.getInstance().markForDeletion(databaseDir);
            if (handler == null) return null;
            handler.setIsEnabled(metaEnabled);
            FGManager.getInstance().addHandler(handler);
            FoxGuardMain.instance().logger().info("Successfully force loaded Handler: " +
                    "(Name: " + name +
                    " Type: " + type +
                    " Priority: " + priority +
                    ")");
            return handler;
        } else if (category.equalsIgnoreCase("region")) {
            World world = listWorld;
            Optional<World> optWorld = Sponge.getGame().getServer().getWorld(metaWorld);
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
            IRegion region = FGFactoryManager.getInstance().createRegion(dataSource, name, type, metaEnabled);
            FGStorageManager.getInstance().markForDeletion(databaseDir);
            if (region == null) return null;
            region.setIsEnabled(metaEnabled);
            FGManager.getInstance().addRegion(world, region);
            FoxGuardMain.instance().logger().info("Successfully force loaded Region: " +
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
        builder.append("Listed Enabled: ").append(listEnabled).append("\n");
        builder.append("Metadata Enabled: ").append(metaEnabled).append("\n");
        if (listWorld != null) builder.append("Listed world: ").append(listWorld.getName()).append("\n");
        if (metaWorld != null) builder.append("Metadata world: ").append(metaWorld).append("\n");
        if (!category.equalsIgnoreCase("region")) builder.append("Priority: ").append(priority).append("\n");
        return builder.toString();
    }
}
