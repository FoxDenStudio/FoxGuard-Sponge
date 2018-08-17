package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.group.LocalGroupElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.group.ServerGroupElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerAdapterFactory;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.*;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by fox on 3/7/18.
 */
public class PathManager {
    private static PathManager ourInstance = new PathManager();
    private Map<String, Class<? extends BaseOwner>> typeClassMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, OwnerAdapterFactory<? extends BaseOwner>> adapterMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, PathOwnerProvider.Literal.Factory<? extends BaseOwner>> literalPathProviderMap = new HashMap<>();
    private Map<String, PathOwnerProvider.Dynamic.Factory<? extends IOwner>> dynamicPathProviderMap = new HashMap<>();

    private Map<UUID, LocalGroupElement> userLocalGroups = new CacheMap<>((key, map) -> {
        if (key instanceof UUID) {
            UUID user = ((UUID) key);
            LocalGroupElement element = new LocalGroupElement(user);
            map.put(user, element);
            return element;
        } else return null;
    });
    private LocalGroupElement serverLocalGroup = new LocalGroupElement(null);
    private ServerGroupElement serverGroup = new ServerGroupElement();

    private Map<String, IPathElement> serverGroupExtensions = new HashMap<>();
    private Map<String, Function<UUID, IPathElement>> localGroupExtensions = new HashMap<>();

    private PathManager() {
        registerOwnerType(UUIDOwner.TYPE, UUIDOwner.class,
                UUIDOwner.Adapter::new,
                UUIDOwner.LiteralPathOwnerProvider::new);
        registerOwnerType(NameOwner.TYPE, NameOwner.class,
                NameOwner.Adapter::new,
                NameOwner.LiteralPathOwnerProvider::new);
        registerOwnerType(NumberOwner.TYPE, NumberOwner.class,
                NumberOwner.Adapter::new,
                NumberOwner.LiteralPathOwnerProvider::new);

        registerServerGroupExtension("s", this.serverLocalGroup);
        registerServerGroupExtension("p", new ServerGroupElement.PlayerAccessNameElement());
        registerServerGroupExtension("o", new ServerGroupElement.PlayerAccessOfflineElement());
        registerServerGroupExtension("u", new ServerGroupElement.PlayerAccessUUIDElement());
    }

    public static PathManager getInstance() {
        return ourInstance;
    }

    public <T extends BaseOwner> boolean registerOwnerType(
            String type,
            Class<T> tClass,
            OwnerAdapterFactory<T> adapterFactory,
            PathOwnerProvider.Literal.Factory<T> pathProviderFactory) {
        if (this.typeClassMap.containsKey(type)) return false;
        this.typeClassMap.put(type, tClass);
        this.adapterMap.put(tClass, adapterFactory);
        this.literalPathProviderMap.put(tClass, pathProviderFactory);
        return true;
    }

    public boolean registerServerGroupExtension(String name, IPathElement provider) {
        if (this.serverGroupExtensions.containsKey(name)) return false;
        this.serverGroupExtensions.put(name, provider);
        return true;
    }

    public boolean registerLocalGroupExtension(String name, Function<UUID, IPathElement> provider) {
        if (this.localGroupExtensions.containsKey(name)) return false;
        this.localGroupExtensions.put(name, provider);
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseOwner> OwnerAdapterFactory<T> getOwnerTypeAdapterFactory(Class<T> tClass) {
        return (OwnerAdapterFactory<T>) this.adapterMap.get(tClass);
    }

    public OwnerAdapterFactory<? extends BaseOwner> getOwnerTypeAdapter(String type) {
        return this.adapterMap.get(this.typeClassMap.get(type));
    }

    public Class<? extends BaseOwner> getTypeClass(String type) {
        return this.typeClassMap.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseOwner> PathOwnerProvider.Literal.Factory<T> getLiteralPathOwnerProvider(Class<T> tClass) {
        return (PathOwnerProvider.Literal.Factory<T>) this.literalPathProviderMap.get(tClass);
    }

    public PathOwnerProvider.Literal.Factory<? extends BaseOwner> getLiteralPathOwnerProvider(String type) {
        return this.getLiteralPathOwnerProvider(this.typeClassMap.get(type));
    }

    public PathOwnerProvider.Dynamic.Factory<? extends IOwner> getDynamicPathOwnerProvider(String type) {
        return this.dynamicPathProviderMap.get(type);
    }

    public LocalGroupElement getServerLocalGroup() {
        return serverLocalGroup;
    }

    public ServerGroupElement getServerGroup() {
        return serverGroup;
    }

    public LocalGroupElement getUserLocalGroup(@Nonnull UUID user) {
        return this.userLocalGroups.get(user);
    }

    public LocalGroupElement getLocalGroup(@Nullable UUID user) {
        return user == null ? getServerLocalGroup() : getUserLocalGroup(user);
    }

    public LocalGroupElement getLocalGroup(@Nullable CommandSource source) {
        return source instanceof User ? getUserLocalGroup(((User) source).getUniqueId()) : getServerLocalGroup();
    }

    public Optional<IPathElement> getServerGroupExtension(String name) {
        return Optional.ofNullable(this.serverGroupExtensions.get(name));
    }

    public Map<String, IPathElement> getServerGroupExtensions() {
        return ImmutableMap.copyOf(serverGroupExtensions);
    }

    public Optional<IPathElement> getLocalGroupExtension(String name, @Nullable UUID user) {
        Function<UUID, IPathElement> func = this.localGroupExtensions.get(name);
        if (func != null) return Optional.ofNullable(func.apply(user));
        else return Optional.empty();
    }

    public Map<String, Function<UUID, IPathElement>> getLocalGroupExtensions() {
        return ImmutableMap.copyOf(localGroupExtensions);
    }

    public Map<String, ? extends IPathElement> getLocalGroupExtensions(@Nullable UUID user) {
        ImmutableMap.Builder<String, IPathElement> map = ImmutableMap.builder();
        for (Map.Entry<String, Function<UUID, IPathElement>> entry : this.localGroupExtensions.entrySet()) {
            IPathElement element = entry.getValue().apply(user);
            if (element != null) {
                map.put(entry.getKey(), element);
            }
        }
        return map.build();
    }
}
