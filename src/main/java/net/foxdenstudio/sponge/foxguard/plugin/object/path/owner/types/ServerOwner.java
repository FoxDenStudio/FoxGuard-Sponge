package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ServerOwner implements IOwner {

    public static final ServerOwner SERVER = new ServerOwner();

    private ServerOwner() {
    }

    @Override
    public Path getDirectory() {
        return Paths.get(".");
    }

    @Override
    public String toString() {
        return "Server-Owner";
    }

    @Override
    public int compareTo(@Nonnull IOwner o) {
        return equals(o) ? 0 : -1;
    }
}
