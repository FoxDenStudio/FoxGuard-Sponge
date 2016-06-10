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

package net.foxdenstudio.sponge.foxguard.plugin.util;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 5/20/2016.
 */
public class ExtraContext {

    private Object[] objects;
    private List<Object> objectList;

    private ExtraContext(Object[] objects) {
        this.objects = objects;
    }

    public static ExtraContext of(Object... objects) {
        return new ExtraContext(objects);
    }

    public boolean present(Class<?> clazz) {
        for (Object o : objects) {
            if (clazz.isInstance(o)) return true;
        }
        return false;
    }

    public <T> Optional<T> first(Class<T> clazz) {
        for (Object o : objects) {
            if (clazz.isInstance(o)) {
                return Optional.of((T) o);
            }
        }
        return Optional.empty();
    }

    public int size() {
        return objects.length;
    }

    public Object get(int index) {
        try {
            return objects[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Range: 0-" + (objects.length - 1) + "  Given: " + index);
        }
    }

    public List<Object> getRaw() {
        if (objectList == null) objectList = ImmutableList.copyOf(objects);
        return objectList;
    }
}
