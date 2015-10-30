package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.Server;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.flags.GlobalFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.GlobalRegion;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class FoxGuardManager {

    private FoxGuardMain plugin;
    private Server server;
    private static FoxGuardManager instance;

    private final Map<World, List<IRegion>> regions;
    private final List<IFlagSet> flagSets;
    private final GlobalFlagSet gfs;


    public FoxGuardManager(FoxGuardMain plugin, Server server) {
        if (instance == null) instance = this;
        this.plugin = plugin;
        this.server = server;
        regions = new HashMap<>();
        flagSets = new LinkedList<>();
        gfs = new GlobalFlagSet();
        this.addFlagSet(gfs);
    }

    public boolean isRegistered(IFlagSet flagSet) {
        return flagSets.contains(flagSet);
    }

    public boolean addRegion(World world, IRegion region) {
        if (region == null) return false;
        if (region.getWorld() != null || getRegion(world, region.getName()) != null) return false;
        region.setWorld(world);
        regions.get(world).add(region);
        FoxGuardStorageManager.getInstance().unmarkForDeletion(region);
        return true;
    }

    public IRegion getRegion(World world, String name) {
        for (IRegion region : regions.get(world)) {
            if (region.getName().equalsIgnoreCase(name)) return region;
        }
        return null;
    }

    public IRegion getRegion(Server server, String worldName, String regionName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() ? this.getRegion(world.get(), regionName) : null;
    }

    public Stream<IRegion> getRegionListAsStream(World world) {
        return this.getRegionsListCopy(world).stream();
    }

    public List<IRegion> getRegionsListCopy() {
        List<IRegion> list = new LinkedList<>();
        this.regions.forEach((world, tlist) -> tlist.forEach(list::add));
        return list;
    }

    public List<IRegion> getRegionsListCopy(World world) {
        List<IRegion> list = new LinkedList<>();
        this.regions.get(world).forEach(list::add);
        return list;
    }

    public List<IFlagSet> getFlagSetsListCopy() {
        List<IFlagSet> list = new LinkedList<>();
        this.flagSets.forEach(list::add);
        return list;
    }

    public boolean addFlagSet(IFlagSet flagSet) {
        if (flagSet == null) return false;
        if (getFlagSet(flagSet.getName()) != null) return false;
        flagSets.add(flagSet);
        FoxGuardStorageManager.getInstance().unmarkForDeletion(flagSet);
        return true;
    }

    public IFlagSet getFlagSet(String name) {
        for (IFlagSet flagSet : flagSets) {
            if (flagSet.getName().equalsIgnoreCase(name)) return flagSet;
        }
        return null;
    }

    public boolean removeFlagSet(String name) {
        return this.removeFlagSet(getFlagSet(name));
    }

    public boolean removeFlagSet(IFlagSet flagSet) {
        if (flagSet == null || flagSet instanceof GlobalFlagSet) return false;
        this.regions.forEach((world, list) -> {
            list.stream()
                    .filter(region -> region.getFlagSets().contains(flagSet))
                    .forEach(region -> region.removeFlagSet(flagSet));
        });
        if (!this.flagSets.contains(flagSet)) return false;
        FoxGuardStorageManager.getInstance().markForDeletion(flagSet);
        flagSets.remove(flagSet);
        return true;
    }

    public boolean removeRegion(World world, String name) {
        return this.removeRegion(world, getRegion(world, name));
    }

    public void removeRegion(IRegion region) {
        if (region.getWorld() != null) removeRegion(region.getWorld(), region);
        else this.regions.forEach((world, list) -> this.removeRegion(world, region));
    }

    public boolean removeRegion(World world, IRegion region) {
        if (region == null || region instanceof GlobalRegion || !this.regions.get(world).contains(region)) return false;
        FoxGuardStorageManager.getInstance().markForDeletion(region);
        this.regions.get(world).remove(region);
        return true;
    }

    public boolean link(Server server, String worldName, String regionName, String flagSetName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.link(world.get(), regionName, flagSetName);
    }

    public boolean link(World world, String regionName, String flagSetName) {
        IRegion region = getRegion(world, regionName);
        IFlagSet flagSet = getFlagSet(flagSetName);
        return this.link(region, flagSet);
    }

    public boolean link(IRegion region, IFlagSet flagSet) {
        if (region == null || flagSet == null || region.getFlagSets().contains(flagSet)) return false;
        if (flagSet instanceof GlobalFlagSet && !(region instanceof GlobalRegion)) return false;
        return region.addFlagSet(flagSet);
    }

    public boolean unlink(Server server, String worldName, String regionName, String flagSetName) {
        Optional<World> world = server.getWorld(worldName);
        return world.isPresent() && this.unlink(world.get(), regionName, flagSetName);
    }

    public boolean unlink(World world, String regionName, String flagSetName) {
        IRegion region = getRegion(world, regionName);
        IFlagSet flagSet = getFlagSet(flagSetName);
        return this.unlink(region, flagSet);
    }

    public boolean unlink(IRegion region, IFlagSet flagSet) {
        if (region == null || flagSet == null || !region.getFlagSets().contains(flagSet)) return false;
        if (flagSet instanceof GlobalFlagSet && region instanceof GlobalRegion) return false;
        return region.removeFlagSet(flagSet);
    }

    public boolean renameRegion(World world, String oldName, String newName) {
        return this.rename(this.getRegion(world, oldName), newName);
    }

    public boolean renameRegion(Server server, String worldName, String oldName, String newName) {
        return this.rename(this.getRegion(server, worldName, oldName), newName);
    }

    public boolean rename(IFGObject object, String newName) {
        if (object instanceof IRegion) {
            IRegion region = (IRegion) object;
            if (this.getRegion(region.getWorld(), newName) != null) return false;
        } else if (object instanceof IFlagSet) {
            if (this.getFlagSet(newName) != null) return false;
        }
        FoxGuardStorageManager.getInstance().markForDeletion(object);
        object.setName(newName);
        FoxGuardStorageManager.getInstance().unmarkForDeletion(object);
        return false;
    }

    public boolean renameFlagSet(String oldName, String newName) {
        return this.rename(this.getFlagSet(oldName), newName);
    }

    public void createLists(World world) {
        regions.put(world, new LinkedList<>());
    }

    public void populateWorld(World world) {
        this.createLists(world);
        GlobalRegion gr = new GlobalRegion();
        gr.addFlagSet(this.gfs);
        addRegion(world, gr);

    }

    public void setup(Server server) {
        server.getWorlds().stream()
                .filter(world -> this.regions.get(world) == null)
                .forEach(this::populateWorld);
    }

    public GlobalFlagSet getGlobalFlagSet() {
        return gfs;
    }

    public static FoxGuardManager getInstance() {
        return instance;
    }

    public Server getServer() {
        return server;
    }
}
