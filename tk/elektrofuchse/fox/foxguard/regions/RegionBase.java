package tk.elektrofuchse.fox.foxguard.regions;

import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/17/2015.
 */
public abstract class RegionBase implements IRegion{

    protected final List<IFlagSet> flagSets;
    String name;

    protected RegionBase(String name, IFlagSet ... flagSets){
        this.name = name;
        this.flagSets = new LinkedList<>();
        if(flagSets != null){
            for(IFlagSet flagSet : flagSets){
                this.flagSets.add(flagSet);
            }
        }
    }

    protected RegionBase(String name) {
        this.name = name;
        this.flagSets = new LinkedList<>();
    }

    @Override
    public List<IFlagSet> getFlagSets() {
        return this.flagSets;
    }

    @Override
    public void addFlagSet(IFlagSet flagSet) {
        this.flagSets.add(flagSet);
    }

    @Override
    public void removeFlagSet(IFlagSet flagSet) {
        this.flagSets.remove(flagSet);
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
