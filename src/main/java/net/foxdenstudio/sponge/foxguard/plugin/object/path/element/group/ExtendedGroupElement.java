package net.foxdenstudio.sponge.foxguard.plugin.object.path.element.group;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;

import java.util.Map;
import java.util.Optional;

public abstract class ExtendedGroupElement extends GroupElement {

    public Optional<? extends IPathElement> getExtension(String name){
        return Optional.ofNullable(this.getExtensions().get(name));
    }

    public abstract Map<String, ? extends IPathElement> getExtensions();

    @Override
    public Optional<? extends IPathElement> resolve(String name, boolean create) {
        if (name.startsWith(">")) return getExtension(name.substring(1));
        else return super.resolve(name, create);
    }
}
