package net.foxdenstudio.sponge.foxguard.plugin.object.provider;

import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.command.CommandSource;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface IObjectProvider {

    /**
     * Method for filtering out objects by filter query.
     *
     * @param objects the collection to filter.
     * @param args    the filter arguments
     * @param source  the command source of the command.
     * @return
     */
    Collection<IFGObject> filter(@Nonnull Collection<IFGObject> objects, @Nonnull String prefix, @Nonnull String[] args, @Nonnull CommandSource source);



    Collection<String> getFilterSuggestions(@Nonnull String prefix, @Nonnull String[] args, @Nonnull  CommandSource source);

    String[] getPrefixes();
}
