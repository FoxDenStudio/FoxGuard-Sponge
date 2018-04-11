package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;

import java.util.Optional;

public interface IModifiablePathElement extends IPathElement {

    @Override
    default Optional<? extends IPathElement> resolve(String name) {
        return resolve(name, false);
    }

    Optional<? extends IPathElement> resolve(String name, boolean create);

    boolean add(String name, IPathElement path);

    default boolean add(IFGObject object) {
        return add(object, object.getName());
    }

    default boolean add(IFGObject object, String name) {
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
    boolean add(IFGObject object, String name, boolean genExtension);

    boolean remove(IFGObject object);

    boolean remove(IPathElement path);

}
