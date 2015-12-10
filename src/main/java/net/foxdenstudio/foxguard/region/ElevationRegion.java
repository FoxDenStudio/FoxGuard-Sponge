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

package net.foxdenstudio.foxguard.region;

import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.foxcore.command.util.ProcessResult;
import net.foxdenstudio.foxcore.command.util.SourceState;
import net.foxdenstudio.foxcore.util.FCHelper;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
                y = FCHelper.parseCoordinate(source instanceof Player ?
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
    public ProcessResult modify(String arguments, SourceState state, CommandSource source) {
        return ProcessResult.failure();
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        try {
            this.lock.readLock().lock();
            return isEnabled && y >= lowerBound && y <= upperBound;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean isInRegion(double x, double y, double z) {
        try {
            this.lock.readLock().lock();
            return isEnabled && y >= lowerBound && y <= upperBound;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = super.getDetails(arguments).builder();
        builder.append(Texts.of(TextColors.GREEN, "\nBounds: "));
        try {
            this.lock.readLock().lock();
            builder.append(Texts.of(TextColors.RESET, lowerBound));
            builder.append(Texts.of(", "));
            builder.append(Texts.of(upperBound));
        } finally {
            this.lock.readLock().unlock();
        }
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
        this.lock.readLock().lock();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(Y INTEGER);" +
                        "DELETE FROM BOUNDS;" +
                        "INSERT INTO BOUNDS(Y) VALUES (" + lowerBound + ");" +
                        "INSERT INTO BOUNDS(Y) VALUES (" + upperBound + ");");
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public int getUpperBound() {
        try {
            this.lock.readLock().lock();
            return upperBound;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void setUpperBound(int upperBound) {
        try {
            this.lock.writeLock().lock();
            this.upperBound = upperBound;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public int getLowerBound() {
        try {
            this.lock.readLock().lock();
            return lowerBound;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void setLowerBound(int lowerBound) {
        try {
            this.lock.writeLock().lock();
            this.lowerBound = lowerBound;
        } finally {
            this.lock.writeLock().unlock();
        }
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
