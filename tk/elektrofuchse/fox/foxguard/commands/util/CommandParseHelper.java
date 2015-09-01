package tk.elektrofuchse.fox.foxguard.commands.util;

import com.google.common.base.Optional;
import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;

/**
 * Created by Fox on 8/22/2015.
 */
public class CommandParseHelper {

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
}
