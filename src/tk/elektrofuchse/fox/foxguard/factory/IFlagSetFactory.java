package tk.elektrofuchse.fox.foxguard.factory;


import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

/**
 * Created by Fox on 10/22/2015.
 */
public interface IFlagSetFactory extends IFGFactory {

    IFlagSet createFlagSet(String type, String name, String arguments, InternalCommandState state, CommandSource source);

}
