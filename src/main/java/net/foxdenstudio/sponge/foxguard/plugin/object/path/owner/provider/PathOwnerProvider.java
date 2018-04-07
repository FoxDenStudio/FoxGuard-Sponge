package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.Owner;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface PathOwnerProvider<T extends Owner> {

    boolean apply(String element);

    default Collection<String> getSuggestions() {
        return this.getSuggestions(this.numApplied());
    }

    default Collection<String> getSuggestions(int index) {
        return ImmutableList.of();
    }

    int numApplied();

    boolean isValid();

    int minimumElements();

    Optional<T> getOwner(String group);

    /**
     * Interface for Provider factory. This class does not normally need to be fully implemented,
     * as a method reference to a no-args constructor is usually sufficient.
     *
     * @param <T>
     */
    interface Factory<T extends Owner> extends Supplier<PathOwnerProvider<T>> {

        @Override
        PathOwnerProvider<T> get();
    }
}
