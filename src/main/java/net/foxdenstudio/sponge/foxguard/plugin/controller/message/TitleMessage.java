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

package net.foxdenstudio.sponge.foxguard.plugin.controller.message;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;

import java.util.Optional;

/**
 * Created by Fox on 4/11/2016.
 */
public class TitleMessage implements ISendableMessage {

    private final Title title;

    public TitleMessage(Title title) {
        this.title = title;
    }

    @Override
    public void send(Player player) {
        player.sendTitle(title);
    }

    @Override
    public Text preview() {
        Text.Builder builder = Text.builder();
        Optional<Text> titleTextOptional = title.getTitle();
        Optional<Text> subtitleTextOptional = title.getSubtitle();
        if (titleTextOptional.isPresent()) {
            builder.append(titleTextOptional.get());
            if (subtitleTextOptional.isPresent()) builder.append(Text.of(TextColors.RESET, " - "));
        }
        if (subtitleTextOptional.isPresent()) builder.append(subtitleTextOptional.get());
        return builder.build();
    }
}
