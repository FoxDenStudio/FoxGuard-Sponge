package net.foxdenstudio.sponge.foxguard.plugin.command.link;

import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;

/**
 * Created by Fox on 4/20/2016.
 */
public class LinkEntry {

    public ILinkable linkable;
    public IHandler handler;

    public LinkEntry(ILinkable linkable, IHandler handler) {
        this.linkable = linkable;
        this.handler = handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinkEntry linkEntry = (LinkEntry) o;

        return linkable != null ? linkable.equals(linkEntry.linkable) : linkEntry.linkable == null && (handler != null ? handler.equals(linkEntry.handler) : linkEntry.handler == null);

    }

    @Override
    public int hashCode() {
        int result = linkable != null ? linkable.hashCode() : 0;
        result = 31 * result + (handler != null ? handler.hashCode() : 0);
        return result;
    }
}
