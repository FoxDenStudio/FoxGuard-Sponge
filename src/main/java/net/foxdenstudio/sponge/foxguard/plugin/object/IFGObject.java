package net.foxdenstudio.sponge.foxguard.plugin.object;

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

    UUID getOwner();

    void setOwner(UUID owner);

    String getFullName();
}
