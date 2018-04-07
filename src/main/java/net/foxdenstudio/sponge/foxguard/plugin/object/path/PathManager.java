package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import com.google.gson.TypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerAdapterFactory;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.NameOwner;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.Owner;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.UUIDOwner;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fox on 3/7/18.
 */
public class PathManager {
    private static PathManager ourInstance = new PathManager();

    public static PathManager getInstance() {
        return ourInstance;
    }

    private PathManager() {
        registerOwnerType(UUIDOwner.TYPE, UUIDOwner.class,
                UUIDOwner.Adapter::new,
                UUIDOwner.PathProvider::new);
        registerOwnerType(NameOwner.TYPE, NameOwner.class,
                NameOwner.Adapter::new,
                NameOwner.PathProvider::new);
    }

    private Map<String, Class<? extends Owner>> typeClassMap = new HashMap<>();
    private Map<Class<? extends Owner>, OwnerAdapterFactory<? extends Owner>> adapterMap = new HashMap<>();
    private Map<Class<? extends Owner>, PathOwnerProvider.Factory<? extends Owner>> literalPathProviderMap = new HashMap<>();

    public <T extends Owner> boolean registerOwnerType(
            String type,
            Class<T> tClass,
            OwnerAdapterFactory<T> adapterFactory,
            PathOwnerProvider.Factory<T> pathProviderFactory) {
        if (this.typeClassMap.containsKey(type)) return false;
        this.typeClassMap.put(type, tClass);
        this.adapterMap.put(tClass, adapterFactory);
        this.literalPathProviderMap.put(tClass, pathProviderFactory);
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T extends Owner> TypeAdapter<T> getOwnerTypeAdapter(Class<T> tClass) {
        return (TypeAdapter<T>) this.adapterMap.get(tClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends Owner> PathOwnerProvider.Factory<T> getLiteralPathOwnerProvider(Class<T> tClass) {
        return (PathOwnerProvider.Factory<T>) this.literalPathProviderMap.get(tClass);
    }

    public PathOwnerProvider.Factory<? extends Owner> getLiteralPathOwnerProvider(String type) {
        return this.getLiteralPathOwnerProvider(this.typeClassMap.get(type));
    }

    public PathResult process(String input) {


        return null;
    }

    enum RootPaths {
        ;

        IPathElement path;

        RootPaths() {

        }
    }

    public static class PathResult {
        String objectName;
        boolean objectPresent;

        IPathElement pathElement;
        boolean pathPresent;
    }
}
