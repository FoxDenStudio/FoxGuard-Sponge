package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public interface IPathOnlyElement extends IPathElement {

    @Override
    default Optional<? extends IFGObject> get(@Nonnull String name, @Nullable World world) {
        return Optional.empty();
    }

    @Override
    default Map<String, ? extends IFGObject> getObjects() {
        return ImmutableMap.of();
    }

    @Override
    default Optional<String> getName(IPathElement path) {
        return Optional.empty();
    }

    @Override
    default boolean isPersistent() {
        return false;
    }
}
