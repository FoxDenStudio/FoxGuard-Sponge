package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Created by fox on 3/4/18.
 */
public abstract class BaseOwner implements IOwner {

    protected final String group;
    protected final String type;

    protected BaseOwner(@Nonnull String type, @Nonnull String group) {
        this.type = type;
        this.group = group;
    }

    public String getType() {
        return this.type;
    }

    public String getGroup() {
        return this.group;
    }

    @Override
    public final Path getDirectory() {
        return Paths.get(FGStorageManagerNew.OWNERS_DIR_NAME, group, type).resolve(getPartialDirectory());
    }

    protected abstract Path getPartialDirectory();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseOwner baseOwner = (BaseOwner) o;
        return Objects.equals(type, baseOwner.type) &&
                Objects.equals(group, baseOwner.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, group);
    }

    @Override
    public int compareTo(@Nonnull IOwner o) {
        if (o == ServerOwner.SERVER) return 1;
        if (o instanceof BaseOwner) {
            BaseOwner baseOwner = ((BaseOwner) o);
            int compare = this.group.compareTo(baseOwner.group);
            if (compare != 0) return compare;
            compare = this.type.compareTo(baseOwner.type);
            return compare;
        } else return 0;
    }

    @Override
    public String toString() {
        return "BaseOwner{" + group + ", " + type + '}';
    }

    public static class Adapter extends TypeAdapter<BaseOwner> {

        private final TypeAdapter<JsonElement> jsonElementTypeAdapter;

        public Adapter(TypeAdapter<JsonElement> jsonElementTypeAdapter) {
            this.jsonElementTypeAdapter = jsonElementTypeAdapter;
        }

        @Override
        public void write(JsonWriter out, BaseOwner value) throws IOException {

        }

        @Override
        public BaseOwner read(JsonReader in) throws IOException {

            return null;
        }
    }

    public static class AdapterFactory implements TypeAdapterFactory {

        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!BaseOwner.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
            Adapter adapter = new Adapter(jsonElementTypeAdapter);
            return (TypeAdapter<T>) adapter;
        }
    }
}
