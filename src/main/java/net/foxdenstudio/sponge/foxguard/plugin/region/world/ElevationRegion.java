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

package net.foxdenstudio.sponge.foxguard.plugin.region.world;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IWorldRegionFactory;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ElevationRegion extends WorldRegionBase {

    private int upperBound;
    private int lowerBound;

    public ElevationRegion(String name, boolean isEnabled, int lowerBound, int upperBound) {
        super(name, isEnabled);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public ElevationRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name, true);
        List<Vector3i> allPositions = new ArrayList<>(positions);
        for (String arg : args) {
            int y;
            try {
                y = FCCUtil.parseCoordinate(source instanceof Player ?
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
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
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
    public List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        Path boundsFile = directory.resolve("bounds.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(boundsFile).build();
        if (Files.exists(boundsFile)) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        root.getNode("lower").setValue(lowerBound);
        root.getNode("upper").setValue(upperBound);
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
        markDirty();
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
        markDirty();
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

    public static class Factory implements IWorldRegionFactory {

        private static final String[] elevAliases = {"elevation", "elev", "height", "y", "vertical", "vert", "level", "updown"};

        @Override
        public IWorldRegion create(String name, String arguments, CommandSource source) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .parse();
            return new ElevationRegion(name, FCPUtil.getPositions(source), parse.args, source);
        }

        @Override
        public IWorldRegion create(Path directory, String name, boolean isEnabled) {
            Path boundsFile = directory.resolve("bounds.cfg");
            CommentedConfigurationNode root;
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(boundsFile).build();
            if (Files.exists(boundsFile)) {
                try {
                    root = loader.load();
                } catch (IOException e) {
                    root = loader.createEmptyNode(ConfigurationOptions.defaults());
                }
            } else {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
            int lower = root.getNode("lower").getInt(0);
            int upper = root.getNode("upper").getInt(0);
            return new ElevationRegion(name, isEnabled, lower, upper);
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
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .excludeCurrent(true)
                    .autoCloseQuotes(true)
                    .parse();
            return ImmutableList.of(parse.current.prefix + "~");
        }
    }
}
