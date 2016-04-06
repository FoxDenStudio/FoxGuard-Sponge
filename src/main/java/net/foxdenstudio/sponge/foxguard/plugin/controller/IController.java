package net.foxdenstudio.sponge.foxguard.plugin.controller;

import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;

import java.nio.file.Path;

public interface IController extends IHandler, ILinkable {

    void loadLinks(Path directory);

    /**
     * Method to get the maximum number of links the controller allows in a given configuration.
     * There is no guarantee that the controller will behave properly if the limit is exceeded.
     * Returns -1 if there is no limit.
     * A value of 0 means that in the current configuration, the controller will not accept links.
     * This should be rare, if not nonexistent.
     *
     * @return The maximum number of links, or -1 if there is no limit.
     */
    int maxLinks();

}
