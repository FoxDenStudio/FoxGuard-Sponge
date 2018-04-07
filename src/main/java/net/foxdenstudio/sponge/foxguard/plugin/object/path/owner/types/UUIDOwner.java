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
import java.util.UUID;

public class UUIDOwner extends Owner {

    public static final String TYPE = "uuid";
    public static final String UUID_REGEX = "[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}";

    UUID uuid;

    public UUIDOwner(String group, UUID uuid) {
        super(TYPE, group);
        this.uuid = uuid;
    }

    @Override
    public Path getPartialDirectory() {
        return Paths.get(uuid.toString());
    }

    public static class Adapter extends OwnerTypeAdapter<UUIDOwner> {

        public Adapter(String group, Gson gson) {
            super(group, gson);
        }

        @Override
        public void write(JsonWriter out, UUIDOwner value) throws IOException {

        }

        @Override
        public UUIDOwner read(JsonReader in) throws IOException {
            return null;
        }
    }

    public static class PathProvider implements PathOwnerProvider<UUIDOwner> {

        UUID uuid = null;
        boolean valid = false;
        int count = 0;

        @SuppressWarnings("Duplicates")
        @Override
        public boolean apply(String element) {
            valid = true;
            if (count++ == 0) {
                if (element != null && !element.isEmpty()) {
                    if (element.matches(UUID_REGEX)) {
                        uuid = UUID.fromString(element);
                        return true;
                    } else {
                        element = element.replace("-", "");
                        if (element.matches("[\\da-f]{32}")) {
                            element = element.substring(0, 8) + "-"
                                    + element.substring(8, 12) + "-"
                                    + element.substring(12, 16) + "-"
                                    + element.substring(16, 20) + "-"
                                    + element.substring(20, 32);
                            uuid = UUID.fromString(element);
                            return true;
                        }
                    }
                }
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
        public Optional<UUIDOwner> getOwner(String group) {
            return this.valid ? Optional.of(new UUIDOwner(group, uuid)) : Optional.empty();
        }
    }
}
