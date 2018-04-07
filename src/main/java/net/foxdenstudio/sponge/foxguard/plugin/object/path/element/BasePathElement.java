package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import org.spongepowered.api.util.GuavaCollectors;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class BasePathElement implements IModifiablePathElement {

    IModifiablePathElement parent;
    Map<String, IPathElement> children;
    Map<String, IGuardObject> objects;

    public BasePathElement(@Nonnull IModifiablePathElement parent) {
        this.parent = parent;
    }

    /**
     * Protected constructor that does not set a parent.
     * While regular instances should always have a parent of some kind,
     */
    protected BasePathElement() {
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name, boolean create) {
        IPathElement ret = this.children.get(name);
        if (ret == null && create) {
            ret = new BasePathElement(this);
            this.children.put(name, ret);
        }
        return Optional.ofNullable(ret);
    }

    @Override
    public boolean addChild(String name, IPathElement path) {
        IPathElement prev = this.children.get(name);
        if (prev != null) return prev == path;

        this.children.put(name, path);
        return true;
    }

    @Override
    public Optional<IGuardObject> get(String name) {
        return this.objects.get(name);
    }

    @Override
    public Collection<String> getPathSuggestions() {
        return this.children.entrySet().stream()
                .filter(e->e.getValue().isPersistent())
                .map(Map.Entry::getKey)
                .sorted()
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public Map<String, IGuardObject> getObjects() {
        return ImmutableMap.copyOf(this.objects);
    }

    @Override
    public boolean isPersistent() {
        for (IPathElement child : this.children.values()) {
            if (child.isPersistent()) return true;
        }
        return false;
    }

    @Override
    public boolean add(IGuardObject object, String name, boolean genExtension) {
        String keyName = name;
        if (genExtension) {
            keyName += "." + object.getPathSuffix();
        }
        if (this.objects.containsKey(keyName)) return false;

        this.objects.put(keyName, object);

        return true;
    }

    @Override
    public boolean remove(IGuardObject object) {
        return this.objects.values().remove(object);
    }

    @Override
    public boolean remove(IPathElement path) {
        return this.children.values().remove(path);
    }

    @Override
    public IModifiablePathElement getParent() {
        return this.parent;
    }

    @Override
    public boolean setParent(IModifiablePathElement path, String newName) {
        if (!path.addChild(newName, this)) return false;
        if (!this.parent.remove(this)) {
            path.remove(this);
            return false;
        }
        return true;
    }
}
