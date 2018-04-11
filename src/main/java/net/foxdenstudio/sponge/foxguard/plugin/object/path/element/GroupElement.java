package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class GroupElement implements IModifiablePathElement {

    Map<String, IPathElement> children;
    Map<String, IFGObject> objects;

    public GroupElement() {

    }

    @Override
    public Optional<? extends IPathElement> resolve(String name, boolean create) {
        IPathElement ret = this.children.get(name);
        if (ret == null && create) {
            ret = new GroupElement();
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
    public Optional<IFGObject> get(@Nonnull String name, @Nullable World world) {
        IFGObject object = this.objects.get(name);
        if (object != null) {
            return Optional.of(object);
        } else return Optional.empty();
    }

    @Override
    public boolean add(IFGObject object, String name, boolean genExtension) {
        String keyName = name;
        if (genExtension) {
            keyName += "." + object.getPathSuffix();
        }
        if (this.objects.containsKey(keyName)) return false;

        this.objects.put(keyName, object);

        return true;
    }

    @Override
    public boolean remove(IFGObject object) {
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
    public Map<String, IFGObject> getObjects() {
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

}
