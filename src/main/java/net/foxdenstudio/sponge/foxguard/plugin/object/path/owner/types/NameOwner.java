package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NameOwner extends SingleKeyComparableOwner<String> {

    public static final String TYPE = "name";

    protected NameOwner(@Nonnull String group, @Nonnull String name) {
        super(TYPE, group, name);
    }

    @Override
    public Path getPartialDirectory() {
        return Paths.get(key);
    }

    @Override
    protected String getPartialCommandPath() {
        return key;
    }

    @Override
    public String toString() {
        return "NameOwner{" + this.group + ", " + this.key + '}';
    }

    public static class Adapter extends OwnerTypeAdapter<NameOwner> {

        public Adapter(String group, Gson gson) {
            super(group, gson);
        }

        @Override
        public void write(JsonWriter out, NameOwner value) {
            gson.toJson(value.key, value.key.getClass(), out);
        }

        @Override
        public NameOwner read(JsonReader in) throws IOException {
            String name = gson.fromJson(in, String.class);
            return new NameOwner(group, name);
        }
    }

    public static class LiteralPathOwnerProvider extends SingleKeyOwner.LiteralPathOwnerProvider<String, NameOwner> {

        @Override
        protected String process(String element) {
            return element;
        }

        @Override
        public Optional<NameOwner> getOwner() {
            return this.valid ? Optional.of(new NameOwner(group, key)) : Optional.empty();
        }
    }
}
