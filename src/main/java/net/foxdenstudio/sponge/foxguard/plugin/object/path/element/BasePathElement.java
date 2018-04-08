package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class BasePathElement implements IPathElement {

    IPathElement parent;

    public BasePathElement(IPathElement parent) {
        this.parent = parent;
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<IGuardObject> get(@Nonnull String name) {
        return Optional.empty();
    }

    @Override
    public Collection<String> getPathSuggestions() {
        return null;
    }

    @Override
    public Map<String, IGuardObject> getObjects() {
        return null;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public IPathElement getParent() {
        return this.parent;
    }
}
