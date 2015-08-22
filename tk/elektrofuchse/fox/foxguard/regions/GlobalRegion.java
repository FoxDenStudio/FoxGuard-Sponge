package tk.elektrofuchse.fox.foxguard.regions;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

/**
 * Created by Fox on 8/17/2015.
 */
public class GlobalRegion extends RegionBase {

    public GlobalRegion() {
        super("global");
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getType() {
        return "Global";
    }

    public boolean isInRegion(int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isInRegion(Vector3i vec) {
        return true;
    }

    @Override
    public boolean isInRegion(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean isInRegion(Vector3d vec) {
        return true;
    }

}
