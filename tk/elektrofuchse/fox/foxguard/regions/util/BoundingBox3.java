package tk.elektrofuchse.fox.foxguard.regions.util;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;

import java.io.Serializable;

/**
 * Created by Fox on 8/17/2015.
 */
public class BoundingBox3 implements Serializable{
    Vector3i a;
    Vector3i b;

    public BoundingBox3(Location parA, Location parB) {
        this(parA.getBlockPosition(), parB.getBlockPosition());
    }

    public BoundingBox3(Vector3i parA, Vector3i parB){
        int ax,ay,az,bx,by,bz;
        if(parA.getX() < parB.getX()){
            ax = parA.getX();
            bx = parB.getX();
        } else {
            ax = parB.getX();
            bx = parA.getX();
        }
        if(parA.getY() < parB.getY()){
            ay = parA.getY();
            by = parB.getY();
        } else {
            ay = parB.getY();
            by = parA.getY();
        }
        if(parA.getZ() < parB.getZ()){
            az = parA.getZ();
            bz = parB.getZ();
        } else {
            az = parB.getZ();
            bz = parA.getZ();
        }
        a = new Vector3i(ax,ay,az);
        b = new Vector3i(bx,by,bz);
    }

    public boolean contains(Vector3i vec){
        return this.contains(vec.getX(), vec.getY(), vec.getZ());
    }
    public boolean contains(Vector3d vec){
        return this.contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(int x, int y, int z){
        return (x >= this.a.getX() && x <= this.b.getX() &&
                y >= this.a.getY() && y <= this.b.getY() &&
                z >= this.a.getZ() && z <= this.b.getZ());
    }

    public boolean contains(double x, double y, double z) {
        return (x > this.a.getX() && x < this.b.getX() + 1 &&
                y > this.a.getY() && y < this.b.getY() + 1 &&
                z > this.a.getZ() && z < this.b.getZ() + 1);
    }


    @Override
    public String toString(){
    return "{(" + a.getX() + ", " + a.getY() + ", " + a.getZ() + "), ("
                + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")}";
    }
}
