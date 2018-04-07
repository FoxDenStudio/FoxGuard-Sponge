package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.PathManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.Owner;

import java.util.*;

public class OwnerPathElement implements IPathElement {

    private static final PathManager manager = PathManager.getInstance();

    private final String prefix;
    private final List<String> currentPath;
    private final String group;
    private final String type;
    private final OwnerPathElement parent;
    private final PathOwnerProvider<? extends Owner> provider;

    public OwnerPathElement(String prefix) {
        this.prefix = prefix;
        this.currentPath = new ArrayList<>();
        this.group = null;
        this.type = null;
        this.parent = null;
        this.provider = null;
    }

    private OwnerPathElement(OwnerPathElement parent, String next) {
        this.prefix = parent.prefix;
        this.parent = parent;
        this.currentPath = new ArrayList<>(parent.currentPath);
        this.currentPath.add(next);
        int size = this.currentPath.size();
        this.group = size > 0 ? this.currentPath.get(0) : null;
        this.type = size > 1 ? this.currentPath.get(1) : null;
        this.provider = size > 1 ? manager.getLiteralPathOwnerProvider(this.type).get() : null;
        for (int i = 2; i < size; i++) {
            String element = this.currentPath.get(i);
            this.provider.apply(element);
        }
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name) {
        return Optional.of(new OwnerPathElement(this, name));
    }

    @Override
    public Optional<IGuardObject> get(String name) {
        if(!this.provider.isValid()) return Optional.empty();

        // TODO actually lookup the object

        return Optional.empty();
    }

    @Override
    public Collection<String> getPathSuggestions() {
        return this.provider.getSuggestions();
    }

    @Override
    public Map<String, IGuardObject> getObjects() {
        if(!this.provider.isValid()) return ImmutableMap.of();

        // TODO actually lookup the object

        return ImmutableMap.of();
    }

    @Override
    public boolean isPersistent() {

        // TODO actually lookup the object
        return false;
    }

    @Override
    public IPathElement getParent() {
        return this.parent;
    }
}
