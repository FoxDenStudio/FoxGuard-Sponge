package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import org.spongepowered.api.util.GuavaCollectors;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class GroupElement implements IModifiablePathElement {

    IPathElement parent;
    String name;
    Map<String, IPathElement> children;
    Map<String, IGuardObject> objects;

    public GroupElement(@Nonnull IModifiablePathElement parent, String name) {
        this.applyParent(parent, name);
    }

    /**
     * Protected constructor that does not set a parent.
     * While regular instances should always have a parent of some kind,
     */
    protected GroupElement() {
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name, boolean create) {
        if (name.equals(".")) {
            return Optional.of(this);
        } else if (name.equals("..")) {
            if (this.parent != null) {
                return Optional.of(this.parent);
            } else {
                return Optional.of(this);
            }
        }
        IPathElement ret = this.children.get(name);
        if (ret == null && create) {
            ret = new GroupElement(this, name);
        }
        return Optional.ofNullable(ret);
    }

    @Override
    public Optional<String> getName(IPathElement path) {
        return Optional.empty();
    }

    @Override
    public boolean add(String name, IPathElement path) {
        if (name.startsWith(".")) return false;

        IPathElement prev = this.children.get(name);
        if (prev != null) return prev == path;

        this.children.put(name, path);
        return true;
    }

    @Override
    public Optional<IGuardObject> get(@Nonnull String name) {
        IGuardObject object = this.objects.get(name);
        if (object != null) {
            return Optional.of(object);
        } else if (this.parent != null && name.isEmpty() || name.startsWith(".")) {
            return this.parent.get(this.name + name);
        } else return Optional.empty();
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
    public Collection<String> getPathSuggestions() {
        return this.children.entrySet().stream()
                .filter(e -> e.getValue().isPersistent())
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
        if (!this.objects.isEmpty()) return true;

        for (IPathElement child : this.children.values()) {
            if (child.isPersistent()) return true;
        }
        return false;
    }

    @Override
    public IPathElement getParent() {
        return this.parent;
    }

    @Override
    public boolean setParent(@Nonnull IPathElement parent) {
        this.parent = parent;
        Optional<String> nameOpt = parent.getName(this);
        this.name = nameOpt.orElse(null);
        return true;
    }

    @Override
    public boolean applyParent(@Nonnull IModifiablePathElement parent, @Nonnull String name) {
        if (this.parent == null) this.parent = parent;
        if (this.name == null) this.name = name;
        if (!parent.add(name, this)) return false;
        this.parent = parent;
        this.name = name;
        return true;
    }
}
