package net.foxdenstudio.sponge.foxguard.plugin;

import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Set;

/**
 * Created by Fox on 4/2/2016.
 */
public interface IFlag {
    @Override
    String toString();

    String flagName();

    IFlag[] getParents();

    /**
     * Gets the hierarchy of the flag. This behavior is very specific, and is already implemented in
     * {@link Flag#getHierarchyStatic(IFlag)} for use by other plugins wishing to implement {@code IFlag}.
     * @return A list of ordered sets representing the hierarchy of the flag.
     */
    List<Set<IFlag>> getHierarchy();

    /**
     *
     * @param input
     * @return
     */
    Tristate resolve(Tristate input);
}
