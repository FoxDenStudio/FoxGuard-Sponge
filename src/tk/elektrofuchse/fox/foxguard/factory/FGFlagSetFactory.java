package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import javax.sql.DataSource;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGFlagSetFactory implements IFlagSetFactory {
    @Override
    public IFlagSet createFlagSet(String name, String type, int priority, String arguments, InternalCommandState state, CommandSource source) {
        return null;
    }

    @Override
    public IFlagSet createFlagSet(DataSource source, String name, String type) {
        return null;
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }
}
