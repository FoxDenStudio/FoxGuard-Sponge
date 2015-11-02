/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
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

package tk.elektrofuchse.fox.foxguard.util;

import org.spongepowered.api.Server;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.flagsets.GlobalFlagSet;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.GlobalRegion;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by Fox on 8/22/2015.
 * Project: foxguard
 */
public class FGHelper {

    public static int parseCoordinate(int sPos, String arg) throws NumberFormatException {
        if (arg.equals("~")) {
            return sPos;
        } else if (arg.startsWith("~")) {
            return sPos + Integer.parseInt(arg.substring(1));
        } else {
            return Integer.parseInt(arg);
        }
    }

    public static Optional<World> parseWorld(String arg, Server server) {
        if (arg.startsWith("w:")) return server.getWorld(arg.substring(2));
        return null;
    }

    public static boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    public static TextColor getColorForRegion(IRegion region) {
        return region.getName().equals(GlobalRegion.NAME) ? TextColors.YELLOW : TextColors.RESET;
    }

    public static TextColor getColorForFlagSet(IFlagSet flagSet) {
        return flagSet.getName().equals(GlobalFlagSet.NAME) ? TextColors.YELLOW : TextColors.RESET;
    }

    @SafeVarargs
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
