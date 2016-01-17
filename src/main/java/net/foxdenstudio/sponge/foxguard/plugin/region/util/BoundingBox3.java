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

package net.foxdenstudio.sponge.foxguard.plugin.region.util;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;

import java.io.Serializable;

public class BoundingBox3 implements Serializable {

    public final Vector3i a;
    public final Vector3i b;

    public BoundingBox3(Location parA, Location parB) {
        this(parA.getBlockPosition(), parB.getBlockPosition());
    }

    public BoundingBox3(Vector3i parA, Vector3i parB) {
        int ax, ay, az, bx, by, bz;
        if (parA.getX() < parB.getX()) {
            ax = parA.getX();
            bx = parB.getX();
        } else {
            ax = parB.getX();
            bx = parA.getX();
        }
        if (parA.getY() < parB.getY()) {
            ay = parA.getY();
            by = parB.getY();
        } else {
            ay = parB.getY();
            by = parA.getY();
        }
        if (parA.getZ() < parB.getZ()) {
            az = parA.getZ();
            bz = parB.getZ();
        } else {
            az = parB.getZ();
            bz = parA.getZ();
        }
        a = new Vector3i(ax, ay, az);
        b = new Vector3i(bx, by, bz);
    }

    public boolean contains(Vector3i vec) {
        return this.contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(Vector3d vec) {
        return this.contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return (x >= this.a.getX() && x <= this.b.getX() &&
                z >= this.a.getZ() && z <= this.b.getZ() &&
                y >= this.a.getY() && y <= this.b.getY());
    }

    public boolean contains(double x, double y, double z) {
        return (x >= this.a.getX() && x <= this.b.getX() + 1 &&
                z >= this.a.getZ() && z <= this.b.getZ() + 1 &&
                y >= this.a.getY() && y <= this.b.getY() + 1);
    }


    @Override
    public String toString() {
        return "{(" + a.getX() + ", " + a.getY() + ", " + a.getZ() + "), ("
                + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")}";
    }
}
