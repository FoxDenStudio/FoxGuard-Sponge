package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.BaseOwner;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface PathOwnerProvider<T extends IOwner> {
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

    Optional<T> getOwner();

    interface Factory<T extends IOwner> extends Supplier<PathOwnerProvider<T>>{

    }

    interface Literal<T extends BaseOwner> extends PathOwnerProvider<T> {

        boolean setGroup(String group);

        /**
         * Interface for Provider factory. This class does not normally need to be fully implemented,
         * as a method reference to a no-args constructor is usually sufficient.
         *
         * @param <T>
         */

        interface Factory<T extends BaseOwner> extends Supplier<Literal<T>> {

            @Override
            Literal<T> get();

        }
    }

}
