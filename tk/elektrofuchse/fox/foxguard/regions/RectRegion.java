package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.util.BoundingBox2;

/**
 * Created by Fox on 8/18/2015.
 */
public class RectRegion extends RegionBase {

    private final BoundingBox2 bb;

    public RectRegion(String name, BoundingBox2 bb) {
        super(name);
        this.bb = bb;
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        return bb.contains(x, z);
    }


    @Override
    public boolean isInRegion(double x, double y, double z) {
        return bb.contains(x, z);
    }

}
