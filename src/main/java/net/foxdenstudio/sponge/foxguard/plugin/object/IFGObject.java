package net.foxdenstudio.sponge.foxguard.plugin.object;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;

import java.util.Comparator;
import java.util.UUID;

public interface IFGObject {

    Comparator<IGuardObject> OWNER_AND_NAME = (o1, o2) -> {
        int ret = o1.getOwner().compareTo(o2.getOwner());
        if (ret != 0) return ret;
        return o1.getName().compareToIgnoreCase(o2.getName());
    };

    /**
     * Gets the name of the object. It should be alphanumeric with limited use of special characters.
     *
     * @return Name of object.
     */
    String getName();

    /**
     * Sets the name of the object. It should be alphanumeric with limited use of special characters.
     *
     * @param name The new name of the object.
     */
    void setName(String name);

    IOwner getOwner();

    void setOwner(IOwner owner);

    /**
     * Gets the path suffix when storing references in paths.
     * This allows objects of completely disparate types to be stored under the same name.
     * Any types that share any hierarchy or can otherwise be used in place of another should have the same suffix.
     * @return the path suffix.
     */
    String getPathSuffix();

    String getFullName();
}
