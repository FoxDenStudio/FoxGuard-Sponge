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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by Fox on 4/2/2016.
 */
public class GlobalRegion extends RegionBase {
    public static final String NAME = "_sglobal";

    public GlobalRegion() {
        super(NAME);
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public boolean contains(int x, int y, int z, World world) {
        return true;
    }

    @Override
    public boolean contains(double x, double y, double z, World world) {
        return true;
    }

    @Override
    public boolean contains(Vector3i vec, World world) {
        return true;
    }

    @Override
    public boolean contains(Vector3d vec, World world) {
        return true;
    }

    @Override
    public boolean isInChunk(Vector3i chunk, World world) {
        return true;
    }

    @Override
    public String getShortTypeName() {
        return "SGlobal";
    }

    @Override
    public String getLongTypeName() {
        return "SuperGlobal";
    }

    @Override
    public String getUniqueTypeString() {
        return "superglobal";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = true;
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        return Text.of("It's global. Nothing to see here. Now move along.");
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
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
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean autoSave() {
        return false;
    }
}
