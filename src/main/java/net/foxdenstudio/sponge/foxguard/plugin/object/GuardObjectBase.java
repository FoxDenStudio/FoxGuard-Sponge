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

package net.foxdenstudio.sponge.foxguard.plugin.object;

import net.foxdenstudio.sponge.foxguard.plugin.FGManager;

import java.util.UUID;

public abstract class GuardObjectBase implements IGuardObject {

    protected String name;
    protected UUID owner;
    protected boolean enabled = true;

    public GuardObjectBase(FGObjectData data) {
        String name = data.getName();
        this.name = (name == null || name.isEmpty()) ? "null" : name;
        UUID owner = data.getOwner();
        this.owner = owner == null ? FGManager.SERVER_UUID : owner;
        this.enabled = data.isEnabled();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public void setOwner(UUID owner) {
        this.owner = owner == null ? FGManager.SERVER_UUID : owner;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    public abstract void markDirty();

    @Override
    public String getFullName() {
        String fullName = this.name;
        if (this.owner != null && !this.owner.equals(FGManager.SERVER_UUID)) {
            fullName = this.owner + ":" + fullName;
        }
        return fullName;
    }

    @Override
    public String toString() {
        return "FGObjectBase{" +
                "name='" + name + '\'' +
                ", owner=" + owner +
                ", enabled=" + enabled +
                '}';
    }
}
