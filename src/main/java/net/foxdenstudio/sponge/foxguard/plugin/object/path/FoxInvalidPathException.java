package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import net.foxdenstudio.sponge.foxguard.plugin.util.FoxException;

import java.util.List;

public class FoxInvalidPathException extends FoxException {

    public FoxInvalidPathException(String element, String prefix, List<String> path) {
        super();
    }

    private static String message(String element, String prefix, List<String> path){
        StringBuilder pathString = new StringBuilder(prefix).append(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            pathString.append("/").append(path.get(i));
        }
        return "Element \"" +  element+ "\" in path \"" + pathString.toString() + "\" " +
                "is invalid" ;
    }
}
