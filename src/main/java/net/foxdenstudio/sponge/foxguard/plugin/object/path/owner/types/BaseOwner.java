package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.PathManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerAdapterFactory;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;
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

    public static final String DEFAULT_GROUP = "unknown";

    protected final String group;
    protected final String type;

    protected BaseOwner(@Nonnull String type, @Nonnull String group) {
        this.type = type;
        this.group = group.isEmpty() ? DEFAULT_GROUP : group;
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

    @Override
    public String getCommandPath() {
        return "::" + this.group + "/" + this.type + "/" + getPartialCommandPath();
    }

    protected abstract Path getPartialDirectory();

    protected abstract String getPartialCommandPath();

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

        public static final String GROUP_KEY = "group";
        public static final String TYPE_KEY = "type";
        public static final String DATA_KEY = "data";

        private final TypeAdapter<JsonElement> jsonElementTypeAdapter;
        private final Gson gson;

        public Adapter(TypeAdapter<JsonElement> jsonElementTypeAdapter, Gson gson) {
            this.jsonElementTypeAdapter = jsonElementTypeAdapter;
            this.gson = gson;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void write(JsonWriter out, BaseOwner value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name(GROUP_KEY);
            out.value(value.group);
            out.name(TYPE_KEY);
            out.value(value.type);
            out.name(DATA_KEY);

            OwnerAdapterFactory factory = PathManager.getInstance().getOwnerTypeAdapterFactory(value.getClass());
            OwnerTypeAdapter adapter = factory.apply(value.group, gson);
            adapter.write(out, value);
            out.endObject();
        }

        @Override
        public BaseOwner read(JsonReader in) throws IOException {
            if(in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String group = null, type = null;
            JsonElement tree = null;
            BaseOwner owner = null;

            in.beginObject();

            while (in.hasNext() && in.peek() != JsonToken.END_OBJECT) {
                String name = in.nextName();
                switch (name) {
                    case GROUP_KEY:
                        group = in.nextString();
                        break;
                    case TYPE_KEY:
                        type = in.nextString();
                        break;
                    case DATA_KEY:
                        if (group == null || group.isEmpty() || type == null || type.isEmpty()) {
                            tree = jsonElementTypeAdapter.read(in);
                        } else {
                            OwnerAdapterFactory<? extends BaseOwner> factory = PathManager.getInstance().getOwnerTypeAdapter(type);
                            OwnerTypeAdapter<? extends BaseOwner> adapter = factory.apply(group, gson);
                            owner = adapter.read(in);
                        }


                }
            }
            in.endObject();

            if (owner != null) return owner;

            if (group == null || group.isEmpty() || type == null || type.isEmpty()) return null;

            if (tree != null) {
                OwnerAdapterFactory<? extends BaseOwner> factory = PathManager.getInstance().getOwnerTypeAdapter(type);
                OwnerTypeAdapter<? extends BaseOwner> adapter = factory.apply(group, gson);
                owner = adapter.fromJsonTree(tree);
            }

            return owner;
        }

    }

}
