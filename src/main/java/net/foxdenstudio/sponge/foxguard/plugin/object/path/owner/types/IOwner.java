package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import java.nio.file.Path;

public interface IOwner extends Comparable<IOwner> {

    Path getDirectory();

    @Override
    String toString();

    @Override
    boolean equals(Object owner);

    @Override
    int hashCode();
}
