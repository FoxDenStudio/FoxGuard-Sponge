package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.PathOwnerProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NameOwner extends Owner {

    public static final String TYPE = "name";

    String name;

    protected NameOwner(String group, String name) {
        super(TYPE, group);
    }

    @Override
    public Path getPartialDirectory() {
        return Paths.get(name);
    }

    public static class Adapter extends OwnerTypeAdapter<NameOwner> {

        public Adapter(String group, Gson gson) {
            super(group, gson);
        }

        @Override
        public void write(JsonWriter out, NameOwner value) throws IOException {

        }

        @Override
        public NameOwner read(JsonReader in) throws IOException {
            return null;
        }
    }

    public static class PathProvider implements PathOwnerProvider<NameOwner> {

        String name = null;
        boolean valid = false;
        int count = 0;

        @Override
        public boolean apply(String element) {
            valid = true;
            if (count++ == 0 && element != null && !element.isEmpty()) {
                name = element;
                return true;
            }
            valid = false;
            return false;
        }

        @Override
        public int numApplied() {
            return count;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public int minimumElements() {
            return 1;
        }

        @Override
        public Optional<NameOwner> getOwner(String group) {
            return this.valid ? Optional.of(new NameOwner(group, this.name)) : Optional.empty();
        }
    }
}
