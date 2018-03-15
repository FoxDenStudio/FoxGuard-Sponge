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

package net.foxdenstudio.sponge.foxguard.plugin.object.owner.provider;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Fox on 8/28/2017.
 * Project: SpongeForge
 */
public class UUIDProvider implements IOwnerProvider {

    public static final String UUID_REGEX = "[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}";
    public static final UUIDProvider INSTANCE = new UUIDProvider();
    private static final String[] ALIASES = {"uuid", "id"};

    private UUIDProvider() {
    }

    public static UUIDProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public List<String> getOwnerKeywords() {
        return ImmutableList.of();
    }

    @Override
    public Optional<UUID> getOwnerUUID(String keyword) {
        if (keyword == null || keyword.isEmpty()) return Optional.empty();
        if (keyword.matches(UUID_REGEX)) {
            return Optional.of(UUID.fromString(keyword));
        } else {
            keyword = keyword.replace("-", "");
            if (!keyword.matches("[\\da-f]{32}")) return Optional.empty();
            keyword = keyword.substring(0, 8) + "-"
                    + keyword.substring(8, 12) + "-"
                    + keyword.substring(12, 16) + "-"
                    + keyword.substring(16, 20) + "-"
                    + keyword.substring(20, 32);
            return Optional.of(UUID.fromString(keyword));
        }
    }

    @Override
    public Optional<String> getKeyword(UUID owner) {
        return Optional.of(owner.toString());
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }
}
