package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider.LiteralPathOwnerProviderBase;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public abstract class SingleKeyOwner<T> extends BaseOwner {

    protected T key;

    protected SingleKeyOwner(@Nonnull String type, @Nonnull String group, @Nonnull T key) {
        super(type, group);
        this.key = key;
    }

    public T getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return "SingleKeyOwner{" + this.group + ", " + this.type + ", " + this.key + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SingleKeyOwner owner = (SingleKeyOwner) o;
        return Objects.equals(this.key, owner.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.key);
    }

    public static abstract class LiteralPathOwnerProvider<T, O extends BaseOwner> extends LiteralPathOwnerProviderBase<O> {

        T key = null;
        boolean valid = false;
        int count = 0;

        @Override
        public boolean apply(String element) {
            valid = true;
            if (count++ == 0 && element != null && !element.isEmpty()) {
                T result = this.process(element);
                if (result != null) {
                    key = result;
                    return true;
                }
            }
            valid = false;
            return false;
        }

        protected abstract T process(String element);

        @Override
        public int numApplied() {
            return count;
        }

        @Override
        public boolean isValid() {
            return this.valid;
        }

        @Override
        public boolean isFinished() {
            return this.valid;
        }

        @Override
        public int minimumElements() {
            return 1;
        }

        @Override
        public abstract Optional<O> getOwner();
    }
}
