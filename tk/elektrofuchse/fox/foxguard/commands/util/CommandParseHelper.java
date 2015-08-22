package tk.elektrofuchse.fox.foxguard.commands.util;

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
}
