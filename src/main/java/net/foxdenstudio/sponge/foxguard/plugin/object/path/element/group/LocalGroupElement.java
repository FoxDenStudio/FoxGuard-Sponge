package net.foxdenstudio.sponge.foxguard.plugin.object.path.element.group;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.PathManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LocalGroupElement extends ExtendedGroupElement {

    public final UUID user;

    public LocalGroupElement(@Nullable UUID user) {
        this.user = user;
    }

    @Override
    public Optional<? extends IPathElement> getExtension(String name) {
        return PathManager.getInstance().getLocalGroupExtension(name, user);
    }

    @Override
    public Map<String, ? extends IPathElement> getExtensions() {
        return PathManager.getInstance().getLocalGroupExtensions(this.user);
    }
}
