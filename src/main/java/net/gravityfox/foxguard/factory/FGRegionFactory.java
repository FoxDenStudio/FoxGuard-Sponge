/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/
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

package net.gravityfox.foxguard.factory;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.regions.CuboidRegion;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.regions.RectangularRegion;
import net.gravityfox.foxguard.regions.util.BoundingBox2;
import net.gravityfox.foxguard.regions.util.BoundingBox3;
import net.gravityfox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGRegionFactory implements IRegionFactory {

    static final String[] rectAliases = {"square", "rectangular", "rectangle", "rect"};
    static final String[] cuboidAliases = {"box", "cube", "cuboid", "cuboidal", "rectangularprism", "rectangleprism", "rectprism"};
    static final String[] types = {"rectangular", "cuboid"};

    @Override
    public IRegion createRegion(String name, String type, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException {
        if (FGHelper.contains(rectAliases, type)) {
            if (source instanceof Player)
                return new RectangularRegion(name, state.positions, arguments.split(" +"), source, (Player) source);
            else return new RectangularRegion(name, state.positions, arguments.split(" +"), source);
        } else if (FGHelper.contains(cuboidAliases, type)) {
            if (source instanceof Player)
                return new CuboidRegion(name, state.positions, arguments.split(" +"), source, (Player) source);
            else return new CuboidRegion(name, state.positions, arguments.split(" +"), source);
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String name, String type) throws SQLException {
        if (type.equalsIgnoreCase("rectangular")) {
            Vector2i a;
            Vector2i b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                ResultSet boundSet = conn.createStatement().executeQuery("SELECT * FROM BOUNDS");
                ResultSet ownerSet = conn.createStatement().executeQuery("SELECT * FROM OWNERS");
                boundSet.next();
                a = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                boundSet.next();
                b = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                while (ownerSet.next()) {
                    Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                    if (user.isPresent()) userList.add(user.get());
                }
            }
            RectangularRegion region = new RectangularRegion(name, new BoundingBox2(a, b));
            region.setOwners(userList);
            return region;
        } else if (type.equalsIgnoreCase("cuboid")) {
            Vector3i a;
            Vector3i b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                ResultSet boundSet = conn.createStatement().executeQuery("SELECT * FROM BOUNDS");
                ResultSet ownerSet = conn.createStatement().executeQuery("SELECT * FROM OWNERS");
                boundSet.next();
                a = new Vector3i(boundSet.getInt("X"), boundSet.getInt("Y"), boundSet.getInt("Z"));
                boundSet.next();
                b = new Vector3i(boundSet.getInt("X"), boundSet.getInt("Y"), boundSet.getInt("Z"));
                while (ownerSet.next()) {
                    Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                    if (user.isPresent()) userList.add(user.get());
                }
            }
            CuboidRegion region = new CuboidRegion(name, new BoundingBox3(a, b));
            region.setOwners(userList);
            return region;
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(rectAliases, cuboidAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}
