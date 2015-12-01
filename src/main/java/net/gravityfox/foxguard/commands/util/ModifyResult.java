/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.commands.util;

import org.spongepowered.api.text.Text;

import java.util.Optional;

/**
 * Created by Fox on 11/30/2015.
 * Project: SpongeForge
 */
public class ModifyResult {

    private static final ModifyResult SUCCESS = of(true);
    private static final ModifyResult FAILURE = of(false);

    private final boolean success;
    private final Optional<Text> message;

    private ModifyResult(boolean success, Optional<Text> message){
        this.success = success;
        this.message = message;
    }

    public static ModifyResult of(boolean success){
        return new ModifyResult(success, Optional.empty());
    }

    public static ModifyResult of(boolean success, Text message) {
        return new ModifyResult(success, Optional.of(message));
    }

    public static ModifyResult success(){
        return SUCCESS;
    }

    public static ModifyResult failure() {
        return FAILURE;
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<Text> getMessage() {
        return message;
    }
}
