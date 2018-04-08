package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import com.google.gson.TypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.OwnerPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.RootGroupElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerAdapterFactory;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.*;
import org.spongepowered.api.command.CommandSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by fox on 3/7/18.
 */
public class PathManager {
    public static final String LITERAL_PREFIX = "::";
    public static final String DYNAMIC_PREFIX = ":";
    private static PathManager ourInstance = new PathManager();
    private Map<String, Class<? extends BaseOwner>> typeClassMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, OwnerAdapterFactory<? extends BaseOwner>> adapterMap = new HashMap<>();
    private Map<Class<? extends BaseOwner>, PathOwnerProvider.Literal.Factory<? extends BaseOwner>> literalPathProviderMap = new HashMap<>();
    private Map<String, PathOwnerProvider.Factory<? extends IOwner>> dynamicPathProviderMap = new HashMap<>();


    private Map<UUID, RootGroupElement> playerLocalGroups = new HashMap<>();

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
    public <T extends BaseOwner> TypeAdapter<T> getOwnerTypeAdapter(Class<T> tClass) {
        return (TypeAdapter<T>) this.adapterMap.get(tClass);
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


    public IGuardObject getObject(String input, String extension, CommandSource source) {


        return null;
    }

    public enum RootPath {

        LITERAL_OWNER(source -> new OwnerPathElement.Literal(LITERAL_PREFIX), LITERAL_PREFIX), // - ::
        DYNAMIC_OWNER(source -> new OwnerPathElement.Dynamic(DYNAMIC_PREFIX), DYNAMIC_PREFIX), // - :
        ;

        public final String prefix;
        Function<CommandSource, IPathElement> rootProvider;

        RootPath(Function<CommandSource, IPathElement> rootProvider, String prefix) {
            this.rootProvider = rootProvider;
            this.prefix = prefix;
        }

        public static RootPath from(String input) {
            if (input == null) return null;
            for (RootPath root : values()) {
                if (input.startsWith(root.prefix)) return root;
            }
            return null;
        }
    }

    public static class PathResult {
        String objectName;
        boolean objectPresent;

        IPathElement pathElement;
        boolean pathPresent;
    }
}
