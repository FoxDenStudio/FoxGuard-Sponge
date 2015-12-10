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

package net.foxdenstudio.foxguard.util;

import net.foxdenstudio.foxcore.commands.FCCommandMainDispatcher;
import net.foxdenstudio.foxguard.handler.GlobalHandler;
import net.foxdenstudio.foxguard.handler.IHandler;
import net.foxdenstudio.foxguard.region.GlobalRegion;
import net.foxdenstudio.foxguard.region.IRegion;
import net.foxdenstudio.foxguard.state.HandlersStateField;
import net.foxdenstudio.foxguard.state.RegionsStateField;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;

public class FGHelper {

    public static TextColor getColorForRegion(IRegion region) {
        return region instanceof GlobalRegion ? TextColors.YELLOW : (region.isEnabled() ? TextColors.RESET : TextColors.GRAY);
    }

    public static TextColor getColorForHandler(IHandler handler) {
        return handler instanceof GlobalHandler ? TextColors.YELLOW : (handler.isEnabled() ? TextColors.RESET : TextColors.GRAY);
    }


    @SuppressWarnings("unchecked")
    public static List<IRegion> getSelectedRegions(CommandSource source) {
        return ((RegionsStateField) FCCommandMainDispatcher.getInstance().getStateMap().get(source).get(RegionsStateField.ID)).getList();
    }

    @SuppressWarnings("unchecked")
    public static List<IHandler> getSelectedHandlers(CommandSource source) {
        return ((HandlersStateField) FCCommandMainDispatcher.getInstance().getStateMap().get(source).get(HandlersStateField.ID)).getList();
    }

}
