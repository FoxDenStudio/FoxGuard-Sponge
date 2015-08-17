package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 */
public interface IRegion{

    boolean isInRegion(int x, int y, int z);

    boolean isInRegion(Vector3i vec);

    List<IFlagSet> getFlagSets();

    void addFlagSet(IFlagSet flagSet);

    void removeFlagSet(IFlagSet flagSet);

    String getName();

    void setName(String name);
}
