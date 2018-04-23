package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class DirectOwnerElement implements IOwnerPathElement {

    @Nonnull
    private final IOwner owner;

    public DirectOwnerElement(@Nonnull IOwner owner) {
        this.owner = owner;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Optional<IOwner> getOwner() {
        return Optional.of(owner);
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends IFGObject> get(@Nonnull String name, @Nullable World world) {
        return owner.getObject(name, world);
    }

    @Override
    public Collection<String> getPathSuggestions() {
        return null;
    }

    @Override
    public Map<String, ? extends IFGObject> getObjects() {
        return null;
    }
}
