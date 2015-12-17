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

package net.foxdenstudio.sponge.foxguard.plugin;

import java.util.concurrent.locks.ReadWriteLock;

public abstract class FGObjectBase implements IFGObject {

    protected final ReadWriteLock lock;
    protected String name;
    protected boolean isEnabled = true;

    protected FGObjectBase(String name) {
        lock = FoxGuardMain.getNewLock();
        this.name = name;
    }

    @Override
    public String getName() {
        String name;
        try {
            this.lock.readLock().lock();
            name = this.name;
        } finally {
            this.lock.readLock().unlock();
        }
        return name;
    }

    @Override
    public void setName(String name) {
        try {
            this.lock.writeLock().lock();
            this.name = name;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isEnabled() {
        boolean isEnabled;
        try {
            this.lock.readLock().lock();
            isEnabled = this.isEnabled;
        } finally {
            this.lock.readLock().unlock();
        }
        return isEnabled;
    }

    @Override
    public void setIsEnabled(boolean state) {
        try {
            this.lock.writeLock().lock();
            this.isEnabled = state;
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
