/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.commands.util;

import com.flowpowered.math.vector.Vector3i;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class InternalCommandState {

    public List<IRegion> selectedRegions = new LinkedList<>();
    public List<IFlagSet> selectedFlagSets = new LinkedList<>();
    public List<Vector3i> positions = new ArrayList<>();

    public void flush() {
        selectedRegions.clear();
        selectedFlagSets.clear();
        positions.clear();
    }

    public void flush(StateField field) {
        switch (field) {
            case REGIONS:
                selectedRegions.clear();
                break;
            case FLAGSETS:
                selectedFlagSets.clear();
                break;
            case POSITIONS:
                positions.clear();
                break;
        }
    }

    public void flush (StateField... fields){
        for(StateField field : fields){
            this.flush(field);
        }
    }


    public enum StateField {
        REGIONS,
        FLAGSETS,
        POSITIONS
    }
}
