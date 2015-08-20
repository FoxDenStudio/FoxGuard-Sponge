package tk.elektrofuchse.fox.foxguard.commands.util;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 */
public class CommandState {

    public List<IRegion> selectedRegions = new LinkedList<>();
    public List<IFlagSet> selectedFlagSets = new LinkedList<>();
    public List<Vector3i> positions = new ArrayList<>();

    public void flush() {
        selectedRegions = new LinkedList<>();
        selectedFlagSets = new LinkedList<>();
        positions = new ArrayList<>();
    }

    public void flush (StateField field){
        switch (field){
            case REGIONS:
                selectedRegions = new LinkedList<>();
                break;
            case FLAGSETS:
                selectedFlagSets = new LinkedList<>();
                break;
            case POSITIONS:
                positions = new ArrayList<>();
                break;
        }
    }


    public enum StateField {
        REGIONS,
        FLAGSETS,
        POSITIONS
    }
}
