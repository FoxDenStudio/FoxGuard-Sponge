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

package net.foxdenstudio.sponge.foxguard.plugin.region;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox3;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IRegionFactory;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CuboidRegion extends RegionBase {

    private final BoundingBox3 boundingBox;


    public CuboidRegion(String name, BoundingBox3 boundingBox) {
        super(name);
        this.boundingBox = boundingBox;
    }

    public CuboidRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name);
        List<Vector3i> allPositions = new ArrayList<>(positions);
        for (int i = 0; i < args.length - 2; i += 3) {
            int x, y, z;
            try {
                x = (int) FCHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockX() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], i);
            }
            try {
                y = (int) FCHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockY() : 0, args[i + 1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + args[i + 1] + "\"!"), e, args[i + 1], i + 1);
            }
            try {
                z = (int) FCHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockZ() : 0, args[i + 2]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + args[i + 2] + "\"!"), e, args[i + 2], i + 2);
            }
            allPositions.add(new Vector3i(x, y, z));
        }
        if (allPositions.isEmpty()) throw new CommandException(Text.of("No parameters specified!"));
        Vector3i a = allPositions.get(0), b = allPositions.get(0);
        for (Vector3i pos : allPositions) {
            a = a.min(pos);
            b = b.max(pos);
        }
        this.boundingBox = new BoundingBox3(a, b);
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) {
        return null;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return boundingBox.contains(x, y, z);
    }


    @Override
    public boolean contains(double x, double y, double z) {
        return boundingBox.contains(x, y, z);
    }

    @Override
    public boolean isInChunk(Vector3i chunk) {
        Vector3i a = chunk.mul(16), b = a.add(16, 16, 16), c = this.boundingBox.a, d = this.boundingBox.b;
        return !(a.getX() > d.getX() || b.getX() < c.getX()
                || a.getZ() > d.getZ() || b.getZ() < c.getZ()
                || a.getY() > d.getY() || b.getY() < c.getY());
    }

    @Override
    public String getShortTypeName() {
        return "Cube";
    }

    @Override
    public String getLongTypeName() {
        return "Cuboid";
    }

    @Override
    public String getUniqueTypeString() {
        return "cuboid";
    }


    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Bounds: "));
        builder.append(Text.of(TextColors.RESET, boundingBox.toString()));
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(X INTEGER, Y INTEGER, Z INTEGER);" +
                        "DELETE FROM BOUNDS;" +
                        "INSERT INTO BOUNDS(X, Y, Z) VALUES (" + boundingBox.a.getX() + ", " + boundingBox.a.getY() + ", " + boundingBox.a.getZ() + ");" +
                        "INSERT INTO BOUNDS(X, Y, Z) VALUES (" + boundingBox.b.getX() + ", " + boundingBox.b.getY() + ", " + boundingBox.b.getZ() + ");");
            }
        }
    }

    @Override
    public String toString() {
        return this.boundingBox.toString();
    }

    public static class Factory implements IRegionFactory {

        private static final String[] cuboidAliases = {"box", "cube", "cuboid", "cuboidal", "rectangularprism", "rectangleprism", "rectprism"};

        @Override
        public IRegion create(String name, String arguments, CommandSource source) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .parse();
            return new CuboidRegion(name, FCHelper.getPositions(source), parse.args, source);
        }

        @Override
        public IRegion create(DataSource source, String name, boolean isEnabled) throws SQLException {
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
            region.setIsEnabled(isEnabled);
            return region;
        }

        @Override
        public String[] getAliases() {
            return cuboidAliases;
        }

        @Override
        public String getType() {
            return "cuboid";
        }

        @Override
        public String getPrimaryAlias() {
            return "cuboid";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .excludeCurrent(true)
                    .autoCloseQuotes(true)
                    .parse();
            return ImmutableList.of(parse.current.prefix + "~");
        }
    }
}
