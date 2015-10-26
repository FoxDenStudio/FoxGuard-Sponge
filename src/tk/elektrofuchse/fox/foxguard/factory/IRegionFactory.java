package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

/**
 * Created by Fox on 10/22/2015.
 */

public interface IRegionFactory extends IFGFactory {

    IRegion createRegion(String type, String name, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException;
}
