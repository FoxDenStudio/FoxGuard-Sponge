package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.BaseOwner;

import java.util.function.BiFunction;

/**
 * Interface for adapter factory. This class does not normally need to be fully implemented,
 * as a method reference to a single {@code String} arg constructor is usually sufficient.
 *
 * @param <T> The type of the owner this factory is supposed to generate adapters for
 */
public interface OwnerAdapterFactory<T extends BaseOwner> extends BiFunction<String, Gson, TypeAdapter<T>> {

    @Override
    OwnerTypeAdapter<T> apply(String group, Gson gson);

}
