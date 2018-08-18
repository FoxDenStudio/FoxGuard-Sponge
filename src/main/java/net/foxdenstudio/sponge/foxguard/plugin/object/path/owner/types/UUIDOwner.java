package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;
import org.spongepowered.api.entity.living.player.User;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public class UUIDOwner extends SingleKeyComparableOwner<UUID> {

    public static final String USER_GROUP = "user";
    public static final String TYPE = "uuid";
    public static final String UUID_REGEX = "[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}";

    public UUIDOwner(@Nonnull String group, @Nonnull UUID uuid) {
        super(TYPE, group, uuid);
    }

    public UUIDOwner(User user) {
        this(USER_GROUP, user.getUniqueId());
    }

    @Override
    public Path getPartialDirectory() {
        return Paths.get(key.toString());
    }

    @Override
    protected String getPartialCommandPath() {
        return key.toString();
    }

    @Override
    public String toString() {
        return "UUIDOwner{" + this.group + ", " + this.key + '}';
    }

    public static class Adapter extends OwnerTypeAdapter<UUIDOwner> {

        public Adapter(String group, Gson gson) {
            super(group, gson);
        }

        @Override
        public void write(JsonWriter out, UUIDOwner value) {
            gson.toJson(value.key, value.key.getClass(), out);
        }

        @Override
        public UUIDOwner read(JsonReader in) {
            UUID uuid = gson.fromJson(in, UUID.class);
            return new UUIDOwner(group, uuid);
        }
    }

    public static class LiteralPathOwnerProvider extends SingleKeyOwner.LiteralPathOwnerProvider<UUID, UUIDOwner> {

        @Override
        protected UUID process(String element) {
            if (element.matches(UUID_REGEX)) {
                return UUID.fromString(element);
            } else {
                element = element.replace("-", "");
                if (element.matches("[\\da-f]{32}")) {
                    element = element.substring(0, 8) + "-"
                            + element.substring(8, 12) + "-"
                            + element.substring(12, 16) + "-"
                            + element.substring(16, 20) + "-"
                            + element.substring(20, 32);
                    return UUID.fromString(element);
                }
            }
            return null;
        }

        @Override
        public Optional<UUIDOwner> getOwner() {
            return this.valid ? Optional.of(new UUIDOwner(group, key)) : Optional.empty();
        }
    }
}
