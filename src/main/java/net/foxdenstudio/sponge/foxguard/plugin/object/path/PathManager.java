package net.foxdenstudio.sponge.foxguard.plugin.object.path;

/**
 * Created by fox on 3/7/18.
 */
public class PathManager {
    private static PathManager ourInstance = new PathManager();

    public static PathManager getInstance() {
        return ourInstance;
    }

    private PathManager() {
    }
}
