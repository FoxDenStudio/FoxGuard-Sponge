package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IModifiablePathElement extends IRelocatablePathElement {

    @Override
    default Optional<? extends IPathElement> resolve(String name) {
        return resolve(name, false);
    }

    Optional<? extends IPathElement> resolve(String name, boolean create);

    boolean add(String name, IPathElement path);

    default boolean add(IGuardObject object) {
        return add(object, object.getName());
    }

    default boolean add(IGuardObject object, String name) {
        return add(object, name, true);
    }

    /**
     * Adds object to this path with alternate name.
     * The genExtension flag controls whether the type extension should be automatically generated.
     *
     * @param object       the object to add.
     * @param name
     * @param genExtension
     * @return
     */
    boolean add(IGuardObject object, String name, boolean genExtension);

    boolean remove(IGuardObject object);

    boolean remove(IPathElement path);

}
