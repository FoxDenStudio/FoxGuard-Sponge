package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/17/2015.
 */
public class GlobalRegion extends RegionBase {

    public GlobalRegion() {
        super("global");
    }

    public GlobalRegion(IFlagSet ... flagSets){
        super("global", flagSets);
    }

    @Override
    public void setName(String name) {}

    public boolean isInRegion(int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isInRegion(Vector3i vec) {
        return true;
    }

}
