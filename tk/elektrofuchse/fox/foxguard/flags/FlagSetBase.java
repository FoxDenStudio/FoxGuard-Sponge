package tk.elektrofuchse.fox.foxguard.flags;

/**
 * Created by Fox on 8/17/2015.
 */
public abstract class FlagSetBase implements IFlagSet {

    protected String name;
    protected int priority;

    public FlagSetBase(String name, int priority) {
        setName(name);
        setPriority(priority);
    }

    public FlagSetBase(){}

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority > 0 ? priority : 1;
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
