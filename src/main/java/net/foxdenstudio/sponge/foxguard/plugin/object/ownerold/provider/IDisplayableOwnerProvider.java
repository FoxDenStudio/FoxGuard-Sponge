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

package net.foxdenstudio.sponge.foxguard.plugin.object.ownerold.provider;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Created by Fox on 8/28/2017.
 * Project: SpongeForge
 */
public interface IDisplayableOwnerProvider extends IOwnerProvider {

    default Optional<String> getDisplayName(UUID owner, @Nullable CommandSource viewer) {
        if (viewer instanceof Player)
            return getKeyword(owner);
        else return getKeyword(owner).map(str->str + "(" + owner.toString() + ")");
    }

    default Optional<Text> getDisplayText(UUID owner, @Nullable CommandSource viewer) {
        return getDisplayName(owner, viewer).map(Text::of);
    }

    default Optional<Text> getHoverText(UUID owner, @Nullable CommandSource viewer) {
        return Optional.of(Text.of(owner.toString()));
    }

}
