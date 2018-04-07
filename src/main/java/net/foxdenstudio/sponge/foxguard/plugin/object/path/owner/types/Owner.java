package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by fox on 3/4/18.
 */
public abstract class Owner {

    private final String type;
    private final String group;

    protected Owner(String type, String group) {
        this.type = type;
        this.group = group;
    }

    public String getType() {
        return this.type;
    }

    public String getGroup() {
        return this.group;
    }

    public Path getDirectory() {
        return Paths.get(group, type).resolve(getPartialDirectory());
    }

    public abstract Path getPartialDirectory();

    public static class Adapter extends TypeAdapter<Owner> {

        private final TypeAdapter<JsonElement> jsonElementTypeAdapter;

        public Adapter(TypeAdapter<JsonElement> jsonElementTypeAdapter) {
            this.jsonElementTypeAdapter = jsonElementTypeAdapter;
        }

        @Override
        public void write(JsonWriter out, Owner value) throws IOException {

        }

        @Override
        public Owner read(JsonReader in) throws IOException {

            return null;
        }
    }

    public static class AdapterFactory implements TypeAdapterFactory {

        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!Owner.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
            Adapter adapter = new Adapter(jsonElementTypeAdapter);
            return (TypeAdapter<T>) adapter;
        }
    }
}
