package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;

/**
 * Created by Fox on 10/25/2015.
 */
public class FGRegionFactory implements IRegionFactory {

    String[] rectAliases = {"rectangular", "rectangle", "rect"};

    @Override
    public IRegion createRegion(String type, String name, String arguments, InternalCommandState state, World world, CommandSource source) throws CommandException {
        if (FGHelper.contains(rectAliases, type)) {
            if(source instanceof Player)
                return new RectRegion(name, state.positions, arguments.split(" "), source, (Player)source);
            else return new RectRegion(name, state.positions, arguments.split(" "), source );
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(rectAliases);
    }
}
