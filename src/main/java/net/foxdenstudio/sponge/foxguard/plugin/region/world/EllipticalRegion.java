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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.BoundingBox2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by Fox on 3/28/2016.
 */
public class EllipticalRegion extends WorldRegionBase {

    private final BoundingBox2 boundingBox;
    private final double centerX, centerY, width, height;
    private final double widthSq, heightSq;

    public EllipticalRegion(String name, boolean isEnabled, double centerX, double centerY, double height, double width) {
        super(name, isEnabled);
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
        this.widthSq = width * width;
        this.heightSq = height * height;
        this.boundingBox = new BoundingBox2(
                new Vector2i(centerX - width / 2, centerY - height / 2),
                new Vector2i(centerX + width / 2, centerY + height / 2));
    }

    @Override
    public boolean contains(int x, int y, int z) {
        double xo = x + 0.5 - centerX, yo = y + 0.5 - centerY;
        return (xo * xo / widthSq) + (yo * yo / heightSq) <= 1;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        double xo = x - centerX, yo = y - centerY;
        return (xo * xo / widthSq) + (yo * yo / heightSq) <= 1;
    }

    @Override
    public String getShortTypeName() {
        return "Elli2D";
    }

    @Override
    public String getLongTypeName() {
        return "Elliptical";
    }

    @Override
    public String getUniqueTypeString() {
        return "elliptical";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Center: "));
        builder.append(Text.of(TextColors.RESET, centerX, ", ", centerY, "\n"));
        builder.append(Text.of(TextColors.GREEN, "Width: "));
        builder.append(Text.of(TextColors.RESET, width, "\n"));
        builder.append(Text.of(TextColors.GREEN, "Height: "));
        builder.append(Text.of(TextColors.RESET, height));
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {

    }


    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean isInChunk(Vector3i chunk) {
        final Vector2i a = chunk.mul(16).toVector2(true), b = a.add(16, 16), c = this.boundingBox.a, d = this.boundingBox.b;
        return !(a.getX() > d.getX() || b.getX() < c.getX() || a.getY() > d.getY() || b.getY() < c.getY());
    }
}
