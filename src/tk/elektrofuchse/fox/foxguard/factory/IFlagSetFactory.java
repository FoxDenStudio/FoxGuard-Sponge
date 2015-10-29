package tk.elektrofuchse.fox.foxguard.factory;


import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;

import javax.sql.DataSource;

/**
 * Created by Fox on 10/22/2015.
 * Project: foxguard
 */
public interface IFlagSetFactory extends IFGFactory {

    IFlagSet createFlagSet(String name, String type,int priority, String arguments, InternalCommandState state, CommandSource source);

    IFlagSet createFlagSet(DataSource source, String type);

}
