package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.OwnerTypeAdapter;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NumberOwner extends SingleKeyComparableOwner<Integer> {

    public static final String TYPE = "number";

    protected NumberOwner(@Nonnull String group, @Nonnull Integer key) {
        super(TYPE, group, key);
    }

    @Override
    protected Path getPartialDirectory() {
        return Paths.get(key.toString());
    }

    @Override
    public String toString() {
        return "NumberOwner{" + this.group + ", " + this.key + '}';
    }

    public static class Adapter extends OwnerTypeAdapter<NumberOwner> {

        public Adapter(String group, Gson gson) {
            super(group, gson);
        }

        @Override
        public void write(JsonWriter out, NumberOwner value) {
            gson.toJson(value.key, value.key.getClass(), out);
        }

        @Override
        public NumberOwner read(JsonReader in) {
            Integer uuid = gson.fromJson(in, Integer.class);
            return new NumberOwner(group, uuid);
        }
    }


    public static class LiteralPathProvider extends SingleKeyOwner.LiteralPathProvider<Integer, NumberOwner> {

        @Override
        protected Integer process(String element) {
            Integer number = null;
            try {
                number = Integer.parseInt(element);
            } catch (NumberFormatException ignored) {
            }
            return number;
        }

        @Override
        public Optional<NumberOwner> getOwner() {
            return this.valid ? Optional.of(new NumberOwner(group, key)) : Optional.empty();
        }
    }
}
