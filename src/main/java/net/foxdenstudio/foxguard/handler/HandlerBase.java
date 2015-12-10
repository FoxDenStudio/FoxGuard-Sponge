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

package net.foxdenstudio.foxguard.handler;

import net.foxdenstudio.foxguard.FGObjectBase;

public abstract class HandlerBase extends FGObjectBase implements IHandler {

    protected int priority;

    public HandlerBase(String name, int priority) {
        super(name);
        setPriority(priority);
    }

    @Override
    public int getPriority() {
        try {
            this.lock.readLock().lock();
            return this.priority;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void setPriority(int priority) {
        try {
            this.lock.writeLock().lock();
            this.priority = priority > Integer.MIN_VALUE ? priority : Integer.MIN_VALUE + 1;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public int compareTo(IHandler o) {
        try {
            this.lock.readLock().lock();
            return o.getPriority() - this.priority;
        } finally {
            this.lock.readLock().unlock();
        }
    }
}
