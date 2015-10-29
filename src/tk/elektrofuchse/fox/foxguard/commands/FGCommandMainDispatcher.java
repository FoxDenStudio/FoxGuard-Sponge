/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.dispatcher.Disambiguator;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.commands.util.CallbackHashMap;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public final class FGCommandMainDispatcher extends FGCommandDispatcher {

    private final Map<CommandSource, InternalCommandState> stateMap = new CallbackHashMap<>((o, m) -> {
        if (o instanceof CommandSource) {
            m.put((CommandSource) o, new InternalCommandState());
        }
    });

    private static FGCommandMainDispatcher instance;
    private final PaginationService pageService;

    public FGCommandMainDispatcher() {
        this(FIRST_DISAMBIGUATOR);
        instance = this;
    }

    public FGCommandMainDispatcher(Disambiguator disambiguatorFunc) {
        super(disambiguatorFunc);
        pageService = FoxGuardMain.getInstance().getGame().getServiceManager().provide(PaginationService.class).get();
    }

    public Map<CommandSource, InternalCommandState> getStateMap() {
        return stateMap;
    }

    public static FGCommandMainDispatcher getInstance() {
        return instance;
    }

    public PaginationService getPageService() {
        return pageService;
    }
}
