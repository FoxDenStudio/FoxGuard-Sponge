package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface IPathElement {

    Optional<? extends IPathElement> resolve(String name);

    Optional<? extends IFGObject> get(@Nonnull String name, @Nullable World world);

    Collection<String> getPathSuggestions();

    Map<String, ? extends IFGObject> getObjects();

    /*default Map<String, ? extends IFGObject> getObjects(String extension){
        ImmutableMap.Builder<String, IFGObject> builder = ImmutableMap.builder();

        for(Map.Entry<String, ? extends IFGObject> entry : getObjects().entrySet()){
            String name = entry.getKey();
            if(name.endsWith(extension)){
                builder.put(entry);
            }
        }

        return builder.build();
    }*/

    /**
     * Check whether this is still persistent.
     * Paths that are no longer persistent don't lead to anything,
     * and can be excluded from suggestions and serialization.
     *
     * @return Whether this path should continue to persist.
     */
    boolean isPersistent();

}
