package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import net.foxdenstudio.sponge.foxcore.common.util.CacheMap;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.RootGroupElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerAdapterFactory;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by fox on 3/7/18.
 */
public class PathManager {
    private static PathManager ourInstance = new PathManager();
    private Map<String, Class<? extends BaseOwner>> typeClassMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, OwnerAdapterFactory<? extends BaseOwner>> adapterMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, PathOwnerProvider.Literal.Factory<? extends BaseOwner>> literalPathProviderMap = new HashMap<>();
    private Map<String, PathOwnerProvider.Factory<? extends IOwner>> dynamicPathProviderMap = new HashMap<>();

    private Map<UUID, RootGroupElement> userLocalGroups = new CacheMap<>((key, map) -> {
        if (key instanceof UUID) {
            RootGroupElement element = RootGroupElement.createLocal();
            map.put(((UUID) key), element);
            return element;
        } else return null;
    });
    private RootGroupElement serverLocalGroup = RootGroupElement.createLocal();
    private RootGroupElement serverGroup = RootGroupElement.createServer();

    private PathManager() {
        registerOwnerType(UUIDOwner.TYPE, UUIDOwner.class,
                UUIDOwner.Adapter::new,
                UUIDOwner.LiteralPathProvider::new);
        registerOwnerType(NameOwner.TYPE, NameOwner.class,
                NameOwner.Adapter::new,
                NameOwner.LiteralPathProvider::new);
        registerOwnerType(NumberOwner.TYPE, NumberOwner.class,
                NumberOwner.Adapter::new,
                NumberOwner.LiteralPathProvider::new);
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

    @SuppressWarnings("unchecked")
    public <T extends BaseOwner> OwnerAdapterFactory<T> getOwnerTypeAdapterFactory(Class<T> tClass) {
        return (OwnerAdapterFactory<T>) this.adapterMap.get(tClass);
    }

    public OwnerAdapterFactory<? extends BaseOwner> getOwnerTypeAdapter(String type) {
        return  this.adapterMap.get(this.typeClassMap.get(type));
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

    public PathOwnerProvider.Factory<? extends IOwner> getDynamicPathOwnerProvider(String type) {
        return this.dynamicPathProviderMap.get(type);
    }

    public RootGroupElement getServerLocalGroup() {
        return serverLocalGroup;
    }

    public RootGroupElement getServerGroup() {
        return serverGroup;
    }

    public RootGroupElement getUserLocalGroup(@Nonnull UUID user) {
        return this.userLocalGroups.get(user);
    }

    public RootGroupElement getLocalGroup(@Nullable UUID user) {
        return user == null ? getServerLocalGroup() : getUserLocalGroup(user);
    }

}
