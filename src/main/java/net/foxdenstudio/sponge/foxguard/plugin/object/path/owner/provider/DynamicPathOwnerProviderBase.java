package net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.provider;

import net.foxdenstudio.sponge.foxguard.plugin.object.path.owner.types.BaseOwner;
import org.spongepowered.api.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class DynamicPathOwnerProviderBase<T extends BaseOwner> implements PathOwnerProvider.Dynamic<T> {

    protected CommandSource source;

    @Override
    public void setSource(@Nullable CommandSource source) {
        this.source = source;
    }

    @Override
    public Optional<CommandSource> getSource() {
        return Optional.ofNullable(source);
    }
}
