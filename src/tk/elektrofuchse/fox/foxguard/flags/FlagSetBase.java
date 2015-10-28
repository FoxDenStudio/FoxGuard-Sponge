package tk.elektrofuchse.fox.foxguard.flags;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public abstract class FlagSetBase implements IFlagSet {

    protected String name;
    protected int priority;
    protected boolean isEnabled = true;

    public FlagSetBase(String name, int priority) {
        setName(name);
        setPriority(priority);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void setIsEnabled(boolean state) {
        this.isEnabled = state;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority > Integer.MIN_VALUE ? priority : Integer.MIN_VALUE + 1;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(IFlagSet o) {
        return o.getPriority() - this.priority;
    }
}
