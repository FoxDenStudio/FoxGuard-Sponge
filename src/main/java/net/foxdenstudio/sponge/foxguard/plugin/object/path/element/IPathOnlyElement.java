package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

public interface IPathOnlyElement extends IRelocatablePathElement {

    @Override
    default Optional<IGuardObject> get(@Nonnull String name) {
        return Optional.empty();
    }

    @Override
    default Map<String, IGuardObject> getObjects() {
        return ImmutableMap.of();
    }
}
