package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.FoxPath;

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
    public String getCommandPath() {
        return FoxPath.SERVER_OWNER_PREFIX;
    }

    @Override
    public int compareTo(@Nonnull IOwner o) {
        return equals(o) ? 0 : -1;
    }

    @Override
    public int hashCode() {
        return 42;
    }
}
