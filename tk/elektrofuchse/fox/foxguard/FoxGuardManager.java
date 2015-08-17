package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.flags.GlobalFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.SimpleFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.GlobalRegion;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Fox on 8/17/2015.
 */
public class FoxGuardManager {

    private FoxGuardMain plugin;
    private static FoxGuardManager instance;

    public final Map<World, List<IRegion>> regions;
    public final Map<World, List<IFlagSet>> flagSets;


    public FoxGuardManager(FoxGuardMain plugin) {
        instance = this;
        this.plugin = plugin;
        regions = new HashMap<>();
        flagSets = new HashMap<>();
    }

    public static FoxGuardManager getInstance() {
        return instance;
    }

    public void loadLists(){}

    public void createLists(World world){
        regions.put(world, new LinkedList<>());
        flagSets.put(world, new LinkedList<>());
    }

    public void populateWorld(World world){
        this.createLists(world);
        GlobalRegion gr = new GlobalRegion();
        GlobalFlagSet gfs = new GlobalFlagSet();
        gr.addFlagSet(gfs);
        regions.get(world).add(gr);
        flagSets.get(world).add(gfs);
    }
}
