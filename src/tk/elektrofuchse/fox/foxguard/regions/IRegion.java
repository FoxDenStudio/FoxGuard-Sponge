package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.IFGObject;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 */
public interface IRegion extends IFGObject {

    boolean modify(String arguments, InternalCommandState state, CommandSource source);

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
