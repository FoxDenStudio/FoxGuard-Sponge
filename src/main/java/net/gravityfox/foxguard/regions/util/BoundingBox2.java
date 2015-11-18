/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package net.gravityfox.foxguard.regions.util;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;

import java.io.Serializable;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class BoundingBox2 implements Serializable {

    public Vector2i a;
    public Vector2i b;

    public BoundingBox2(Location parA, Location parB) {
        this(parA.getBlockPosition(), parB.getBlockPosition());
    }

    public BoundingBox2(Vector2i parA, Vector2i parB) {
        int ax, ay, bx, by;
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
        a = new Vector2i(ax, ay);
        b = new Vector2i(bx, by);
    }

    public BoundingBox2(Vector3i parA, Vector3i parB) {
        int ax, ay, bx, by;
        if (parA.getX() < parB.getX()) {
            ax = parA.getX();
            bx = parB.getX();
        } else {
            ax = parB.getX();
            bx = parA.getX();
        }
        if (parA.getZ() < parB.getZ()) {
            ay = parA.getZ();
            by = parB.getZ();
        } else {
            ay = parB.getZ();
            by = parA.getZ();
        }
        a = new Vector2i(ax, ay);
        b = new Vector2i(bx, by);
    }

    public boolean contains(Vector2i vec) {
        return this.contains(vec.getX(), vec.getY());
    }

    public boolean contains(Vector2d vec) {
        return this.contains(vec.getX(), vec.getY());
    }

    public boolean contains(int x, int y) {
        return (x >= this.a.getX() && x <= this.b.getX() &&
                y >= this.a.getY() && y <= this.b.getY());
    }

    public boolean contains(double x, double y) {
        return (x > this.a.getX() && x < this.b.getX() + 1 &&
                y > this.a.getY() && y < this.b.getY() + 1);
    }


    @Override
    public String toString() {
        return "{(" + a.getX() + ", " + a.getY() + "), ("
                + b.getX() + ", " + b.getY() + ")}";
    }
}
