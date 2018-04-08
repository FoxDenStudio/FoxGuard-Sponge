package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import javax.annotation.Nonnull;

public interface IRelocatablePathElement extends IPathElement {

    boolean applyParent(@Nonnull IModifiablePathElement parent, @Nonnull String newName);

    boolean setParent(@Nonnull IPathElement parent);
}
