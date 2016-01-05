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

package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.region.CuboidRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.ElevationRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.RectangularRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.util.BoundingBox2;
import net.foxdenstudio.sponge.foxguard.plugin.region.util.BoundingBox3;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class FGRegionFactory implements IRegionFactory {

    private static final String[] rectAliases = {"square", "rectangular", "rectangle", "rect"};
    private static final String[] cuboidAliases = {"box", "cube", "cuboid", "cuboidal", "rectangularprism", "rectangleprism", "rectprism"};
    private static final String[] elevAliases = {"elevation", "elev", "height", "y", "vertical", "vert", "level", "updown"};
    private static final String[] types = {"rectangular", "cuboid", "elevation"};

    @Override
    public String[] getAliases() {
        return FCHelper.concatAll(rectAliases, cuboidAliases, elevAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }

    @Override
    public String[] getPrimaryAliases() {
        return types;
    }

    @Override
    public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (isIn(FCHelper.concatAll(rectAliases, cuboidAliases, elevAliases), type)) {
            return ImmutableList.of(parse.current.prefix + "~");
        } else return ImmutableList.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public IRegion createRegion(String name, String type, String arguments, CommandSource source) throws CommandException {
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                .arguments(arguments)
                .parse();
        if (isIn(rectAliases, type)) {
            if (source instanceof Player)
                return new RectangularRegion(name, FCHelper.getPositions(source), parse.args, source, (Player) source);
            else
                return new RectangularRegion(name, FCHelper.getPositions(source), parse.args, source);
        } else if (isIn(cuboidAliases, type)) {
            if (source instanceof Player)
                return new CuboidRegion(name, FCHelper.getPositions(source), parse.args, source, (Player) source);
            else
                return new CuboidRegion(name, FCHelper.getPositions(source), parse.args, source);
        } else if (isIn(elevAliases, type)) {
            if (source instanceof Player)
                return new ElevationRegion(name, FCHelper.getPositions(source), parse.args, source, (Player) source);
            else
                return new ElevationRegion(name, FCHelper.getPositions(source), parse.args, source);
        } else return null;
    }

    @Override
    public IRegion createRegion(DataSource source, String name, String type, boolean isEnabled) throws SQLException {
        if (type.equalsIgnoreCase("rectangular")) {
            Vector2i a, b;
            List<User> userList = new ArrayList<>();
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
            List<User> userList = new ArrayList<>();
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
            List<User> userList = new ArrayList<>();
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
}
