/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public abstract class RegionBase implements IRegion {

    protected final List<IFlagSet> flagSets;
    protected String name;
    protected World world;

    public RegionBase(String name) {
        this.name = name;
        this.flagSets = new LinkedList<>();
    }

    @Override
    public boolean isInRegion(Vector3i vec) {
        return this.isInRegion(vec.getX(), vec.getY(), vec.getZ());
    }

    @Override
    public boolean isInRegion(Vector3d vec) {
        return this.isInRegion(vec.getX(), vec.getY(), vec.getZ());
    }

    @Override
    public List<IFlagSet> getFlagSets() {
        return this.flagSets;
    }

    @Override
    public boolean addFlagSet(IFlagSet flagSet) {
        if (flagSets.contains(flagSet) || !FoxGuardManager.getInstance().isRegistered(flagSet)) return false;
        this.flagSets.add(flagSet);
        return true;
    }

    @Override
    public boolean removeFlagSet(IFlagSet flagSet) {
        if (!flagSets.contains(flagSet)) return false;
        this.flagSets.remove(flagSet);
        return true;
    }

    @Override
    public void setWorld(World world) {
        if (this.world == null) this.world = world;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

}
