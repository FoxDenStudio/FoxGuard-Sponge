package net.foxdenstudio.sponge.foxguard.plugin.object.path;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.DirectLocalOwnerElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IOwnerPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.OwnerPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class FoxPath {

    public static final String LOCAL_ALT_CHAR = "~";

    public static final String LITERAL_PREFIX = "::";
    public static final String DYNAMIC_PREFIX = ":";
    public static final String SERVER_OWNER_PREFIX = "://";
    public static final String LOCAL_OWNER_PREFIX = ":/";
    public static final String LOCAL_OWNER_PREFIX_ALT = ":" + LOCAL_ALT_CHAR + "/";
    public static final String SERVER_GROUP_PREFIX = "//";
    public static final String LOCAL_GROUP_PREFIX = "/";
    public static final String WORKING_PATH_PREFIX = "./";

    public static final Root DEFAULT_ROOT = Root.SERVER_OWNER;

    private static final PathManager PATH_MANAGER = PathManager.getInstance();

    private int depth = 0;
    private Root root;
    private List<String> pathNames = new ArrayList<>();
    private List<IPathElement> pathElements = new ArrayList<>();

    FoxPath(@Nonnull Root rootPath, @Nullable CommandSource source) {
        this(rootPath, rootPath.prefixes[0], source);
    }

    FoxPath(@Nonnull String rootPrefix, @Nullable CommandSource source) {
        this(Root.from(rootPrefix), rootPrefix, source);
    }

    private FoxPath(@Nonnull Root rootPath, @Nonnull String prefix, @Nullable CommandSource source) {
        this.root = rootPath;
        this.pathElements.add(rootPath.apply(source));
        this.pathNames.add(prefix);
    }

    @Nonnull
    public ResolveResult resolve(@Nonnull String element) {
        if (element.equals(".")) return ResolveResult.SUCCESS;
        else if (element.equals("..")) {
            goUp();
            return ResolveResult.SUCCESS;
        } else if (element.startsWith(".")) return ResolveResult.FAILED;

        IPathElement currentElement = this.pathElements.get(depth);
        if (currentElement == null) return ResolveResult.FAILED;

        Optional<? extends IPathElement> newElementOpt = currentElement.resolve(element);
        if (newElementOpt.isPresent()) {
            this.pathElements.add(newElementOpt.get());
            this.pathNames.add(element);
            this.depth++;
            return ResolveResult.SUCCESS;
        } else {
            this.pathElements.add(null);
            this.pathNames.add(element);
            this.depth++;
            return ResolveResult.PHANTOM;
        }
    }

    public IPathElement getCurrentElement() {
        return this.pathElements.get(this.depth);
    }


    public static Optional<IFGObject> getObject(@Nonnull String input, @Nullable CommandSource source, @Nullable String extension, @Nullable World world) {
        if (input.isEmpty()) return Optional.empty();
        Root rootPath = Root.from(input);

        boolean explicitPath;
        if (rootPath == null) {
            explicitPath = false;
            rootPath = Root.getDefault();
        } else {
            explicitPath = true;
            input = rootPath.trimPrefix(input);
        }

        if (input.isEmpty()) return Optional.empty();
        String[] sections = input.split(":+", explicitPath ? 3 : 2);
        if (sections.length == 0) return Optional.empty();

        String pathStr;
        String filterStr;
        String name;

        int count = sections.length;
        if (explicitPath) {
            pathStr = sections[0];
            --count;
        } else {
            pathStr = null;
        }

        if (count != 0) {
            name = sections[sections.length - 1];
            --count;
        } else {
            name = null;
        }

        if (count != 0) {
            filterStr = sections[sections.length - 2];
        } else {
            filterStr = null;
        }

        FoxPath foxPath = new FoxPath(rootPath, source);

        if (explicitPath) {
            String[] pathParts = pathStr.split("/+");
            ResolveResult resolveResult = ResolveResult.SUCCESS;
            for (String part : pathParts) {
                if (part.isEmpty()) continue;
                resolveResult = foxPath.resolve(part);
            }
            if (resolveResult != ResolveResult.SUCCESS) {
                if (resolveResult == ResolveResult.PHANTOM && name == null) {
                    name = pathParts[pathParts.length - 1];
                    foxPath.goUp();
                }
                return Optional.empty();
            }
        }


        return Optional.empty();
    }

    public static Optional<? extends IOwner> getOwner(@Nonnull String input, @Nullable CommandSource source) {
        if (input.isEmpty()) return Optional.empty();
        Root rootPath = Root.from(input);
        if (rootPath == null) {
            rootPath = Root.getDefault();
        } else {
            input = rootPath.trimPrefix(input);
        }

        //if (input.endsWith(":")) input = input.substring(0, input.length() - 1);
        String[] parts = input.split("[/:]+");

        FoxPath foxPath = new FoxPath(rootPath, source);
        IPathElement element;
        Optional<? extends IOwner> owner = Optional.empty();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            element = foxPath.getCurrentElement();
            if (element instanceof IOwnerPathElement) {
                IOwnerPathElement ownerElement = ((IOwnerPathElement) element);
                Optional<? extends IOwner> newOwner;
                if (ownerElement.isFinished()) {
                    newOwner = ownerElement.getOwner();
                    if (newOwner.isPresent()) return newOwner;
                } else if (ownerElement.isValid()) {
                    newOwner = ownerElement.getOwner();
                    if (newOwner.isPresent()) owner = newOwner;
                }
            }
            foxPath.resolve(part);
        }
        element = foxPath.getCurrentElement();
        if (element instanceof IOwnerPathElement && ((IOwnerPathElement) element).isValid()) {
            Optional<? extends IOwner> newOwner = ((IOwnerPathElement) element).getOwner();
            if (newOwner.isPresent()) owner = newOwner;
        }

        return owner;
    }

    private void goUp() {
        if (depth == 0) return;

        pathNames.remove(depth);
        pathElements.remove(depth);
        depth--;
    }

    enum ResolveResult {
        SUCCESS, FAILED, PHANTOM
    }

    public enum Root implements Function<CommandSource, IPathElement> {

        SERVER_OWNER(new DirectLocalOwnerElement.Suppier(true), SERVER_OWNER_PREFIX), // ->  ://
        LOCAL_OWNER(new DirectLocalOwnerElement.Suppier(false), LOCAL_OWNER_PREFIX, LOCAL_OWNER_PREFIX_ALT), // ->  :/  :~/
        LITERAL_OWNER(source -> new OwnerPathElement.Literal(LITERAL_PREFIX), LITERAL_PREFIX), // ->  ::
        DYNAMIC_OWNER(source -> new OwnerPathElement.Dynamic(DYNAMIC_PREFIX), DYNAMIC_PREFIX, LOCAL_ALT_CHAR + DYNAMIC_PREFIX), // ->  :  ~:
        SERVER_GROUP(source -> PATH_MANAGER.getServerGroup(), SERVER_GROUP_PREFIX), // ->  //
        LOCAL_GROUP(PATH_MANAGER::getLocalGroup, LOCAL_GROUP_PREFIX, LOCAL_ALT_CHAR + LOCAL_GROUP_PREFIX), // ->  /  ~/
        //WORKING_PATH(),
        ;

        public final String[] prefixes;
        private final Function<CommandSource, IPathElement> rootProvider;

        Root(Function<CommandSource, IPathElement> rootProvider, String... prefixes) {
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

        public static Root from(String input) {
            if (input == null) return null;
            for (Root root : values()) {
                for (String prefix : root.prefixes) {
                    if (input.startsWith(prefix)) return root;
                }
            }
            return null;
        }

        public static Root getDefault() {
            return SERVER_OWNER;
        }

        @Override
        public IPathElement apply(@Nullable CommandSource source) {
            return rootProvider.apply(source);
        }
    }
}
