package net.foxdenstudio.sponge.foxguard.plugin.object.owner.type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by fox on 3/4/18.
 */
public abstract class Owner {

    private final String type;

    protected Owner(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

    public abstract Path getPath();

    protected abstract void serializeData(JsonWriter out);

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
            if(!Owner.class.isAssignableFrom(type.getRawType())) return null;
            TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
            Adapter adapter = new Adapter(jsonElementTypeAdapter);
            return (TypeAdapter<T>) adapter;
        }
    }
}
