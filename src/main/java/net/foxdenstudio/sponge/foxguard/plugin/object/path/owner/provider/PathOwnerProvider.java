package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.BaseOwner;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.IOwner;
import org.spongepowered.api.command.CommandSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PathOwnerProvider<T extends IOwner> {
    boolean apply(String element);

    default Collection<String> getSuggestions() {
        return ImmutableList.of();
    }

    int numApplied();

    boolean isValid();

    boolean isFinished();

    int minimumElements();

    Optional<T> getOwner();

    interface Dynamic<T extends IOwner> extends PathOwnerProvider<T>{

        void setSource(@Nullable CommandSource source);

        Optional<CommandSource> getSource();

        /**
         * Interface for Provider factory. This class does not normally need to be fully implemented,
         * as a method reference to a single arg constructor of type {@code CommandSource} is usually sufficient.
         *
         * @param <T> The particular type of {@code IOwner} this factory generates providers for.
         */

        interface Factory<T extends IOwner> extends Supplier< Dynamic<T>> {

            @Override
            Dynamic<T> get();
        }
    }

    interface Literal<T extends BaseOwner> extends PathOwnerProvider<T> {

        boolean setGroup(@Nonnull String group);

        String getGroup();

        /**
         * Interface for a Literal Provider factory. This class does not normally need to be fully implemented,
         * as a method reference to a no-args constructor is usually sufficient.
         *
         * @param <T> The particular type of {@code BaseOwner} this factory generates providers for.
         */

        interface Factory<T extends BaseOwner> extends Supplier<Literal<T>> {

            @Override
            Literal<T> get();

        }
    }

}
