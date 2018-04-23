package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public interface IOwner extends Comparable<IOwner> {

    Path getDirectory();

    @Override
    String toString();

    @Override
    boolean equals(Object owner);

    @Override
    int hashCode();

    default Optional<? extends IFGObject> getObject(@Nonnull String name, @Nullable World world) {
        FGManager manager = FGManager.getInstance();



        return Optional.empty();
    }

}
