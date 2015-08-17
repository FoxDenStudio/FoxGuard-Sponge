package tk.elektrofuchse.fox.foxguard.flags;

import org.spongepowered.api.entity.player.Player;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.flags.util.PassiveFlags;

/**
 * Created by Fox on 8/17/2015.
 */
public class GlobalFlagSet extends FlagSetBase{

    public GlobalFlagSet() {
        setPriority(0);
        setName("global");
    }

    @Override
    public void setPriority(int priority) {
        this.priority = 0;
    }

    @Override
    public void setName(String name) {
        this.name = "global";
    }

    @Override
    public FlagState hasPermission(Player player, ActiveFlags flag) {
        return FlagState.TRUE;
    }

    @Override
    public FlagState isEnabled(PassiveFlags flag) {
        return FlagState.TRUE;
    }
}
