package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.FoxInvalidPathException;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface IPathElement {

    Optional<? extends IPathElement> resolve(String name);

    Optional<String> getName(IPathElement path);

    Optional<IGuardObject> get(@Nonnull String name);

    Collection<String> getPathSuggestions();

    Map<String, IGuardObject> getObjects();

    /**
     * Check whether this is still persistent.
     * Paths that are no longer persistent don't lead to anything,
     * and can be excluded from suggestions and serialization.
     *
     * @return Whether this path should continue to persist.
     */
    boolean isPersistent();

    IPathElement getParent();
}
