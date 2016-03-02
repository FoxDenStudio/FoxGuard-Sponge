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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox2;
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

public class ElevationRegion extends RegionBase {

    private int upperBound;
    private int lowerBound;

    public ElevationRegion(String name, int lowerBound, int upperBound) {
        super(name);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public ElevationRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name);
        List<Vector3i> allPositions = new ArrayList<>(positions);
        for (String arg : args) {
            int y;
            try {
                y = (int) FCHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockY() : 0, arg);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + arg + "\"!"), e, arg, 1);
            }
            allPositions.add(new Vector3i(0, y, 0));
        }
        if (allPositions.isEmpty()) throw new CommandException(Text.of("No parameters specified!"));
        int a = allPositions.get(0).getY(), b = allPositions.get(0).getY();
        for (Vector3i pos : allPositions) {
            a = Math.min(a, pos.getY());
            b = Math.max(b, pos.getY());
        }
        this.lowerBound = a;
        this.upperBound = b;
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return y >= lowerBound && y <= upperBound;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return y >= lowerBound && y <= upperBound + 1;
    }

    @Override
    public boolean isInChunk(Vector3i chunk) {
        int a = chunk.getY() * 16, b = a + 16;
        return !(a > this.upperBound || b < this.lowerBound);
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Bounds: "));
        builder.append(Text.of(TextColors.RESET, lowerBound));
        builder.append(Text.of(", "));
        builder.append(Text.of(upperBound));
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
                statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(Y INTEGER);" +
                        "DELETE FROM BOUNDS;" +
                        "INSERT INTO BOUNDS(Y) VALUES (" + lowerBound + ");" +
                        "INSERT INTO BOUNDS(Y) VALUES (" + upperBound + ");");
            }
        }
    }

    public int getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    @Override
    public String getShortTypeName() {
        return "Elev";
    }

    @Override
    public String getLongTypeName() {
        return "Elevation";
    }

    @Override
    public String getUniqueTypeString() {
        return "elevation";
    }

    public static class Factory implements IRegionFactory {

        private static final String[] elevAliases = {"elevation", "elev", "height", "y", "vertical", "vert", "level", "updown"};

        @Override
        public IRegion create(String name, String arguments, CommandSource source) throws CommandException {
            AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                    .arguments(arguments)
                    .parse();
            return new ElevationRegion(name, FCHelper.getPositions(source), parse.args, source);
        }

        @Override
        public IRegion create(DataSource source, String name, boolean isEnabled) throws SQLException {
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
            region.setIsEnabled(isEnabled);
            return region;
        }

        @Override
        public String[] getAliases() {
            return elevAliases;
        }

        @Override
        public String getType() {
            return "elevation";
        }

        @Override
        public String getPrimaryAlias() {
            return "elevation";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            AdvCmdParse.ParseResult parse = AdvCmdParse.builder()
                    .arguments(arguments)
                    .excludeCurrent(true)
                    .autoCloseQuotes(true)
                    .parse();
            return ImmutableList.of(parse.current.prefix + "~");
        }
    }
}
