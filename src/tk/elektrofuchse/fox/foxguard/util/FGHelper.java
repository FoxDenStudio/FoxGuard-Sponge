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
