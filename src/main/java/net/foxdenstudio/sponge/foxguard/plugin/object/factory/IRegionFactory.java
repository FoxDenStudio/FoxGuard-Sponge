package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import java.nio.file.Path;

/**
 * Created by Fox on 3/31/2016.
 */
public interface IRegionFactory extends IFGFactory {

    IRegion create(String name, String arguments, CommandSource source) throws CommandException;

    IRegion create(Path directory, String name, boolean isEnabled);
}
