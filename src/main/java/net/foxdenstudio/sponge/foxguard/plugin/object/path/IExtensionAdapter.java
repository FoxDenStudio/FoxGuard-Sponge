package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Adapter used when trying to resolve object using an owner.
 * Because owners don't hold onto references to objects
 */
public interface IExtensionAdapter {

    Optional<? extends IFGObject> getObject(@Nonnull String nameWithExtension);

}
