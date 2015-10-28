package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fox on 10/22/2015.
 */
public class FGFactoryManager {

    private final List<IRegionFactory> regionFactories;
    private final List<IFlagSetFactory> flagSetFactories;


    private static FGFactoryManager ourInstance = new FGFactoryManager();

    public static FGFactoryManager getInstance() {
        return ourInstance;
    }

    private FGFactoryManager() {
        regionFactories = new ArrayList<>();
        regionFactories.add(new FGRegionFactory());
        flagSetFactories = new ArrayList<>();
        flagSetFactories.add(new FGFlagSetFactory());
    }


    public IRegion createRegion(String name, String type, String args, InternalCommandState state, World world, CommandSource source) throws CommandException {
        for (IRegionFactory rf : regionFactories) {
            if (FGHelper.contains(rf.getAliases(), type)) {
                return rf.createRegion(name, type, args, state, world, source);
            }
        }
        return null;
    }

    public IRegion createRegion(DataSource source, String type){
        for (IRegionFactory rf : regionFactories) {
            if (FGHelper.contains(rf.getAliases(), type)) {
                return rf.createRegion(source, type);
            }
        }
        return null;
    }


    public IFlagSet createFlagSet(String name, String type, int priority, String args, InternalCommandState state, CommandSource source) {
        for (IFlagSetFactory fsf : flagSetFactories) {
            if (FGHelper.contains(fsf.getAliases(), type)) {
                return fsf.createFlagSet(name, type, priority, args, state, source);
            }
        }
        return null;
    }

    public IFlagSet createFlagSet(DataSource source, String type) {
        for (IFlagSetFactory fsf : flagSetFactories) {
            if (FGHelper.contains(fsf.getAliases(), type)) {
                return fsf.createFlagSet(source, type);
            }
        }
        return null;
    }

    public boolean registerRegionFactory(IRegionFactory factory) {
        if (regionFactories.contains(factory)) return false;
        regionFactories.add(factory);
        return true;
    }

    public boolean registerFlagSetFactory(IFlagSetFactory factory) {
        if (flagSetFactories.contains(factory)) return false;
        flagSetFactories.add(factory);
        return true;
    }

    public boolean unregister(Object factory) {
        if (factory instanceof IRegionFactory)
            return regionFactories.remove(factory);
        else if (factory instanceof IFlagSetFactory)
            return flagSetFactories.remove(factory);
        return false;
    }
}
