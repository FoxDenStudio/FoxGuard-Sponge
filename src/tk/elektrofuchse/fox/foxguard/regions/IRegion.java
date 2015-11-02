/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.IFGObject;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;

import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 * Project: foxguard
 */
public interface IRegion extends IFGObject {

    boolean isInRegion(int x, int y, int z);

    boolean isInRegion(Vector3i vec);

    boolean isInRegion(double x, double y, double z);

    boolean isInRegion(Vector3d vec);

    List<IFlagSet> getFlagSets();

    boolean addFlagSet(IFlagSet flagSet);

    boolean removeFlagSet(IFlagSet flagSet);

    void setWorld(World world);

    World getWorld();

}
