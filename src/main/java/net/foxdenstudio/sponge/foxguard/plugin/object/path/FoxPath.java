package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.DirectOwnerElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.OwnerPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.RootGroupElement;
import net.minecraft.world.World;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class FoxPath {

    public static final String LOCAL_ALT_PREPREFIX = "~";

    public static final String LITERAL_PREFIX = "::";
    public static final String DYNAMIC_PREFIX = ":";
    public static final String SERVER_OWNER_PREFIX = "//:";
    public static final String LOCAL_OWNER_PREFIX = "/:";
    public static final String SERVER_GROUP_PREFIX = "//";
    public static final String LOCAL_GROUP_PREFIX = "/";
    public static final String WORKING_PATH_PREFIX = "./";

    public static final RootPath DEFAULT_ROOT = RootPath.SERVER_OWNER;

    private static final PathManager PATH_MANAGER = PathManager.getInstance();

    private int depth = 0;
    private RootPath rootPath;
    private List<String> pathNames = new ArrayList<>();
    private List<IPathElement> pathElements = new ArrayList<>();

    FoxPath(@Nonnull RootPath rootPath, @Nullable CommandSource source) {
        this(rootPath, rootPath.prefixes[0], source);
    }

    FoxPath(@Nonnull String rootPrefix, @Nullable CommandSource source) {
        this(RootPath.from(rootPrefix), rootPrefix, source);
    }

    private FoxPath(@Nonnull RootPath rootPath, @Nonnull String prefix, @Nullable CommandSource source) {
        this.rootPath = rootPath;
        this.pathElements.add(rootPath.rootProvider.apply(source));
        this.pathNames.add(prefix);
    }

    @Nonnull
    public Result resolve(@Nonnull String element) {
        if (element.equals(".")) return Result.SUCCESS;
        else if (element.equals("..")) {
            goUp();
            return Result.SUCCESS;
        } else if (element.startsWith(".")) return Result.FAILED;

        IPathElement currentElement = this.pathElements.get(depth);
        if (currentElement == null) return Result.FAILED;

        Optional<? extends IPathElement> newElementOpt = currentElement.resolve(element);
        if (newElementOpt.isPresent()) {
            this.pathElements.add(newElementOpt.get());
            this.pathNames.add(element);
            this.depth++;
            return Result.SUCCESS;
        } else {
            this.pathElements.add(null);
            this.pathNames.add(element);
            this.depth++;
            return Result.PHANTOM;
        }
    }


    public static Optional<IFGObject> fromUserInput(@Nonnull String input, @Nullable String extension, @Nullable World world) {
        RootPath rootPath = RootPath.from(input);
        input = rootPath.trimPrefix(input);
        if (input.isEmpty()) return Optional.empty();
        String[] parts = input.split(":+", 2);

        if (extension == null) extension = "";

        return null;
    }

    private void goUp() {
        if (depth == 0) return;

        pathNames.remove(depth);
        pathElements.remove(depth);
        depth--;
    }

    enum Result {
        SUCCESS, FAILED, PHANTOM
    }

    public enum RootPath {

        LITERAL_OWNER(source -> new OwnerPathElement.Literal(LITERAL_PREFIX), LITERAL_PREFIX), // - ::
        DYNAMIC_OWNER(source -> new OwnerPathElement.Dynamic(DYNAMIC_PREFIX), DYNAMIC_PREFIX, '~' + DYNAMIC_PREFIX), // - :
        SERVER_OWNER(new DirectOwnerElement.Suppier(true), SERVER_OWNER_PREFIX),
        LOCAL_OWNER(new DirectOwnerElement.Suppier(false)),
        SERVER_GROUP(source -> PATH_MANAGER.getServerGroup()),
        LOCAL_GROUP(source -> source instanceof User ?
                PATH_MANAGER.getUserLocalGroup(((User) source).getUniqueId()) :
                PATH_MANAGER.getServerLocalGroup()),
        //WORKING_PATH(),
        ;

        public final String[] prefixes;
        Function<CommandSource, IPathElement> rootProvider;

        RootPath(Function<CommandSource, IPathElement> rootProvider, String... prefixes) {
            this.rootProvider = rootProvider;
            this.prefixes = prefixes;
        }

        public String getPrefix(String input) {
            for (String prefix : this.prefixes) {
                if (input.startsWith(prefix)) return prefix;
            }
            return "";
        }

        public String trimPrefix(String input) {
            return input.substring(getPrefix(input).length());
        }

        public static RootPath from(String input) {
            if (input == null) return null;
            for (RootPath root : values()) {
                for (String prefix : root.prefixes) {
                    if (input.startsWith(prefix)) return root;
                }
            }
            return null;
        }
    }
}
