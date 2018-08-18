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

package net.foxdenstudio.sponge.foxguard.plugin.storage;

import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by Fox on 7/9/2017.
 * Project: SpongeForge
 */
public class FGSObjectMeta extends FGSObjectPath {

    String category;
    String type;

    public FGSObjectMeta(String name, IOwner owner, String category, String type) {
        super(name, owner);
        this.category = category;
        this.type = type;
    }

    public FGSObjectMeta() {
    }

    public FGSObjectMeta(IGuardObject object) {
        super(object);
        this.category = FGUtil.getCategory(object);
        this.type = object.getUniqueTypeString();
        this.object = object;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FGSObjectMeta{");
        sb.append("category='").append(category).append("', ");
        sb.append("type='").append(type).append("', ");
        sb.append("name='").append(name).append("', ");
        sb.append("owner='").append(owner).append("', ");
        if (object != null) sb.append(", object=").append(object);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FGSObjectMeta that = (FGSObjectMeta) o;
        return Objects.equals(category, that.category) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), category, type);
    }
}
