package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.BaseOwner;

import javax.annotation.Nonnull;

public abstract class LiteralPathOwnerProviderBase<T extends BaseOwner> implements PathOwnerProvider.Literal<T> {

    protected String group = "null";

    @Override
    public boolean setGroup(@Nonnull String group) {
        if (group.isEmpty()) return false;
        this.group = group;
        return true;
    }

    @Override
    public String getGroup() {
        return this.group;
    }
}
