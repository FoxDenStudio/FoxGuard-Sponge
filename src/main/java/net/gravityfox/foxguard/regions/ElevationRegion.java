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

package net.gravityfox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 11/25/2015.
 * Project: SpongeForge
 */
public class ElevationRegion extends OwnableRegionBase {

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
        List<Vector3i> allPositions = new LinkedList<>(positions);
        for (int i = 0; i < args.length; i++) {
            int y;
            try {
                y = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockY() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], 1);
            }
            allPositions.add(new Vector3i(0, y, 0));
        }
        if (allPositions.isEmpty()) throw new CommandException(Texts.of("No parameters specified!"));
        int a = allPositions.get(0).getY(), b = allPositions.get(0).getY();
        for (Vector3i pos : allPositions) {
            a = Math.min(a, pos.getY());
            b = Math.max(b, pos.getY());
        }
        this.lowerBound = a;
        this.upperBound = b;
    }

    public ElevationRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, User... owners) throws CommandException {
        this(name, positions, args, source);
        Collections.addAll(ownerList, owners);
    }

    public ElevationRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, List<User> owners) throws CommandException {
        this(name, positions, args, source);
        this.ownerList = owners;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        return y >= lowerBound && y <= upperBound;
    }

    @Override
    public boolean isInRegion(double x, double y, double z) {
        return y > lowerBound && y < upperBound + 1;
    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = super.getDetails(arguments).builder();
        builder.append(Texts.of(TextColors.GREEN, "\nBounds: "));
        builder.append(Texts.of(TextColors.RESET, lowerBound));
        builder.append(Texts.of(", "));
        builder.append(Texts.of(upperBound));
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
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
}
