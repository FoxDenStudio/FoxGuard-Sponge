package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface IOwner extends Comparable<IOwner> {

    Path getDirectory();

    @Override
    String toString();

    /**
     * Get the absolute string to reference this owner, including prefix
     * @return
     */
    String getCommandPath();

    @Override
    boolean equals(Object owner);

    @Override
    int hashCode();

    default Optional<? extends IFGObject> getObject(@Nonnull String name, @Nullable World world) {
        FGManager manager = FGManager.getInstance();



        return Optional.empty();
    }

    class Adapter extends TypeAdapter<IOwner> {

        private final TypeAdapter<BaseOwner> baseOwnerTypeAdapter;
        private final Gson gson;

        public Adapter(TypeAdapter<BaseOwner> baseOwnerTypeAdapter, Gson gson) {
            this.baseOwnerTypeAdapter = baseOwnerTypeAdapter;
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, IOwner value) throws IOException {
            if (value == null || value instanceof ServerOwner) {
                out.nullValue();
            } else if (value instanceof BaseOwner) {
                baseOwnerTypeAdapter.write(out, (BaseOwner) value);
            } else {
                out.nullValue();
                FoxGuardMain.instance().getLogger().warn("Failed to serialize an IOwner that isn't a BaseOwner or ServerOwner. Defaulting to server owner");
            }
        }

        @Override
        public IOwner read(JsonReader in) throws IOException {
            if(in.peek() == JsonToken.NULL){
                in.nextNull();
                return ServerOwner.SERVER;
            } else {
                return baseOwnerTypeAdapter.read(in);
            }
        }
    }

    class Factory implements TypeAdapterFactory {

        public static final Factory INSTANCE = new Factory();

        private Factory() {
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (BaseOwner.class.isAssignableFrom(type.getRawType())) {
                TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
                BaseOwner.Adapter adapter = new BaseOwner.Adapter(jsonElementTypeAdapter, gson);
                return (TypeAdapter<T>) adapter;
            } else if (IOwner.class.equals(type.getRawType()) || ServerOwner.class.equals(type.getRawType())) {
                TypeAdapter<BaseOwner> baseOwnerAdapter = gson.getAdapter(BaseOwner.class);
                IOwner.Adapter adapter = new IOwner.Adapter(baseOwnerAdapter, gson);
                return (TypeAdapter<T>) adapter;
            } else return null;
        }
    }
}
