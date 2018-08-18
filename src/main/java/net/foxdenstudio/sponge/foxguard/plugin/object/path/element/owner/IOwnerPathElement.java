package net.foxdenstudio.sponge.foxguard.plugin.object.path.element.owner;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;

import java.util.Optional;

public interface IOwnerPathElement extends IPathElement {

    boolean isFinished();

    boolean isValid();

    @Override
    default boolean isPersistent() {
        return false;
    }

    Optional<? extends IOwner> getOwner();

}
