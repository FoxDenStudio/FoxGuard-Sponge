package net.foxdenstudio.sponge.foxguard.plugin.object.path.element.owner;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
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

public class DirectLocalOwnerElement implements IOwnerPathElement {

    private final UUID uuid;
    private final IOwner owner;

    public DirectLocalOwnerElement(@Nullable UUID uuid) {
        this.uuid = uuid;
        owner = uuid == null ? FGManager.SERVER_OWNER : new UUIDOwner(UUIDOwner.USER_GROUP, uuid);
    }

    @Override
    public Optional<? extends IPathElement> resolve(String name) {
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
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Optional<IOwner> getOwner() {
        return Optional.of(owner);
    }

    public static class Suppier implements Function<CommandSource, IPathElement> {

        private final Map<UUID, DirectLocalOwnerElement> cache = new CacheMap<>((key, map) -> {
            if (key == null || key instanceof UUID) {
                UUID uuid = ((UUID) key);
                DirectLocalOwnerElement element = new DirectLocalOwnerElement(uuid);
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
            return !this.server && source instanceof User ?
                    cache.get(((User) source).getUniqueId()) :
                    cache.get(null);
        }
    }
}
