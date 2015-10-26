package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

/**
 * Created by Fox on 10/25/2015.
 */
public class FGFlagSetFactory implements IFlagSetFactory {
    @Override
    public IFlagSet createFlagSet(String type, String name, String arguments, InternalCommandState state, CommandSource source) {
        return null;
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }
}
