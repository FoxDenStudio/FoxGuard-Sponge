package net.foxdenstudio.sponge.foxguard.plugin.object.path.element;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.UUIDOwner;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class DirectOwnerElement implements IPathElement {

    private final UUID uuid;
    private final UUIDOwner owner;

    public DirectOwnerElement(UUID uuid) {
        this.uuid = uuid;
        owner = new UUIDOwner(UUIDOwner.USER_GROUP, this.uuid);
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getName(IPathElement path) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends IFGObject> get(@Nonnull String name, @Nullable World world) {
        //TODO fetch object from manager
        return Optional.empty();
    }

    @Override
    public Collection<String> getPathSuggestions() {
        return ImmutableList.of();
    }

    @Override
    public Map<String, ? extends IFGObject> getObjects() {
        //TODO fetch objects from manager.
        return null;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public boolean finished() {
        return true;
    }

    public static class Suppier implements Function<CommandSource, IPathElement> {

        private final Map<UUID, DirectOwnerElement> cache = new CacheMap<>((key, map) -> {
            if (key instanceof UUID) {
                UUID uuid = ((UUID) key);
                DirectOwnerElement element = new DirectOwnerElement(uuid);
                map.put(uuid, element);
                return element;
            } else return null;
        });
        private final boolean server;

        public Suppier(boolean server) {
            this.server = server;
        }

        @Override
        public IPathElement apply(CommandSource source) {
            return source instanceof User && !this.server ?
                    cache.get(((User) source).getUniqueId()) :
                    cache.get(null);
        }
    }
}
