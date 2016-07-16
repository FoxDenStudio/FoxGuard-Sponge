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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox2;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IWorldRegionFactory;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RectangularRegion extends WorldRegionBase {

    private BoundingBox2 boundingBox;


    public RectangularRegion(String name, boolean isEnabled, BoundingBox2 boundingBox) {
        super(name, isEnabled);
        this.boundingBox = boundingBox;
    }

    public RectangularRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name, true);
        List<Vector3i> allPositions = new ArrayList<>(positions);
        for (int i = 0; i < args.length - 1; i += 2) {
            int x, z;
            try {
                x = FCCUtil.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockX() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], i);
            }
            try {
                z = FCCUtil.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockZ() : 0, args[i + 1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Text.of("Unable to parse \"" + args[i + 1] + "\"!"), e, args[i + 1], i + 1);
            }
            allPositions.add(new Vector3i(x, 0, z));
        }
        if (allPositions.isEmpty()) throw new CommandException(Text.of("No parameters specified!"));
        Vector3i a = allPositions.get(0), b = allPositions.get(0);
        for (Vector3i pos : allPositions) {
            a = a.min(pos);
            b = b.max(pos);
        }
        this.boundingBox = new BoundingBox2(a, b);
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
        return boundingBox.contains(x, z);
    }


    @Override
    public boolean contains(double x, double y, double z) {
        return boundingBox.contains(x, z);
    }

    @Override
    public boolean isInChunk(Vector3i chunk) {
        final Vector2i a = chunk.mul(16).toVector2(true), b = a.add(16, 16), c = this.boundingBox.a, d = this.boundingBox.b;
        return !(a.getX() > d.getX() || b.getX() < c.getX() || a.getY() > d.getY() || b.getY() < c.getY());
    }

    @Override
    public String getShortTypeName() {
        return "Rect";
    }

    @Override
    public String getLongTypeName() {
        return "Rectangular";
    }

    @Override
    public String getUniqueTypeString() {
        return "rectangular";
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
        root.getNode("lowerX").setValue(boundingBox.a.getX());
        root.getNode("lowerZ").setValue(boundingBox.a.getY());
        root.getNode("upperX").setValue(boundingBox.b.getX());
        root.getNode("upperZ").setValue(boundingBox.b.getY());
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BoundingBox2 getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox2 boundingBox) {
        this.boundingBox = boundingBox;
        markDirty();
    }

    public static class Factory implements IWorldRegionFactory {

        private static final String[] rectAliases = {"square", "rectangular", "rectangle", "rect"};

        @Override
        public IWorldRegion create(String name, String arguments, CommandSource source) throws CommandException {
            AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                    .arguments(arguments)
                    .parse();
            return new RectangularRegion(name, FCPUtil.getPositions(source), parse.args, source);
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
            int x1 = root.getNode("lowerX").getInt(0);
            int z1 = root.getNode("lowerZ").getInt(0);
            int x2 = root.getNode("upperX").getInt(0);
            int z2 = root.getNode("upperZ").getInt(0);
            return new RectangularRegion(name, isEnabled, new BoundingBox2(new Vector2i(x1, z1), new Vector2i(x2, z2)));
        }

        @Override
        public String[] getAliases() {
            return rectAliases;
        }

        @Override
        public String getType() {
            return "rectangular";
        }

        @Override
        public String getPrimaryAlias() {
            return "rectangular";
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
