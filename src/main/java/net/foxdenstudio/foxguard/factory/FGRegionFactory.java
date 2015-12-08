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

package net.foxdenstudio.foxguard.factory;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.foxcommon.commands.util.SourceState;
import net.foxdenstudio.foxcommon.state.PositionsStateField;
import net.foxdenstudio.foxcommon.util.FCHelper;
import net.foxdenstudio.foxguard.FoxGuardMain;
import net.foxdenstudio.foxguard.regions.CuboidRegion;
import net.foxdenstudio.foxguard.regions.ElevationRegion;
import net.foxdenstudio.foxguard.regions.IRegion;
import net.foxdenstudio.foxguard.regions.RectangularRegion;
import net.foxdenstudio.foxguard.regions.util.BoundingBox2;
import net.foxdenstudio.foxguard.regions.util.BoundingBox3;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.World;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.foxdenstudio.foxcommon.util.Aliases.isAlias;

public class FGRegionFactory implements IRegionFactory {

    static final String[] rectAliases = {"square", "rectangular", "rectangle", "rect"};
    static final String[] cuboidAliases = {"box", "cube", "cuboid", "cuboidal", "rectangularprism", "rectangleprism", "rectprism"};
    static final String[] elevAliases = {"elevation", "elev", "height", "y", "vertical", "vert", "level", "updown"};
    static final String[] types = {"rectangular", "cuboid", "elevation"};

    @SuppressWarnings("unchecked")
    @Override
    public IRegion createRegion(String name, String type, String arguments, SourceState state, World world, CommandSource source) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (isAlias(rectAliases, type)) {
            if (source instanceof Player)
                return new RectangularRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source, (Player) source);
            else
                return new RectangularRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source);
        } else if (isAlias(cuboidAliases, type)) {
            if (source instanceof Player)
                return new CuboidRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source, (Player) source);
            else
                return new CuboidRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source);
        } else if (isAlias(elevAliases, type)) {
            if (source instanceof Player)
                return new ElevationRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source, (Player) source);
            else
                return new ElevationRegion(name, ((PositionsStateField) state.get(PositionsStateField.ID)).getList(), args, source);
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String name, String type, boolean isEnabled) throws SQLException {
        if (type.equalsIgnoreCase("rectangular")) {
            Vector2i a, b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet boundSet = statement.executeQuery("SELECT * FROM BOUNDS")) {
                        boundSet.next();
                        a = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                        boundSet.next();
                        b = new Vector2i(boundSet.getInt("X"), boundSet.getInt("Z"));
                    }
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent()) userList.add(user.get());
                        }
                    }
                }

            }
            RectangularRegion region = new RectangularRegion(name, new BoundingBox2(a, b));
            region.setOwners(userList);
            region.setIsEnabled(isEnabled);
            return region;
        } else if (type.equalsIgnoreCase("cuboid")) {
            Vector3i a, b;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet boundSet = statement.executeQuery("SELECT * FROM BOUNDS")) {
                        boundSet.next();
                        a = new Vector3i(boundSet.getInt("X"), boundSet.getInt("Y"), boundSet.getInt("Z"));
                        boundSet.next();
                        b = new Vector3i(boundSet.getInt("X"), boundSet.getInt("Y"), boundSet.getInt("Z"));
                    }
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent()) userList.add(user.get());
                        }
                    }
                }
            }
            CuboidRegion region = new CuboidRegion(name, new BoundingBox3(a, b));
            region.setOwners(userList);
            region.setIsEnabled(isEnabled);
            return region;
        } else if (type.equalsIgnoreCase("elevation")) {
            int lowerBound, upperBound;
            List<User> userList = new LinkedList<>();
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet boundSet = statement.executeQuery("SELECT * FROM BOUNDS")) {
                        boundSet.next();
                        lowerBound = boundSet.getInt("Y");
                        boundSet.next();
                        upperBound = boundSet.getInt("Y");
                    }
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent()) userList.add(user.get());
                        }
                    }
                }

            }
            ElevationRegion region = new ElevationRegion(name, lowerBound, upperBound);
            region.setOwners(userList);
            region.setIsEnabled(isEnabled);
            return region;
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FCHelper.concatAll(rectAliases, cuboidAliases, elevAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}
