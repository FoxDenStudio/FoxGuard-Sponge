package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types;

import javax.annotation.Nonnull;

public abstract class SingleKeyComparableOwner<T extends Comparable<T>> extends SingleKeyOwner<T> {

    protected SingleKeyComparableOwner(@Nonnull String type, @Nonnull String group, @Nonnull T key) {
        super(type, group, key);
    }

    @Override
    public int compareTo(@Nonnull IOwner o) {
        if (o == ServerOwner.SERVER) return 1;
        if (o instanceof BaseOwner) {
            BaseOwner baseOwner = ((BaseOwner) o);
            int compare = super.compareTo(baseOwner);
            if (compare != 0) return compare;
            if (o instanceof SingleKeyComparableOwner) {
                SingleKeyComparableOwner<? extends Comparable> comparableOwner = ((SingleKeyComparableOwner<? extends Comparable>) o);
                if (this.key.getClass() == comparableOwner.key.getClass()) {
                    //noinspection unchecked
                    return this.key.compareTo((T) comparableOwner.key);
                } else throw new IllegalStateException("Can't compare two keys of different types! " +
                        "Expected: " + this.key.getClass() +
                        ", Actual: " + comparableOwner.key.getClass());
            } else
                throw new IllegalStateException("All owners of same type must be the same class! Type-id: \"" + this.type
                        + "\", Expected-class: " + this.getClass()
                        + ", Actual-class: " + o.getClass());
        } else
            throw new IllegalStateException("All IOwner instances that aren't the server owner must extend BaseOwner");
    }
}
