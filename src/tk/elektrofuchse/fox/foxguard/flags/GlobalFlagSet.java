package tk.elektrofuchse.fox.foxguard.flags;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.flags.util.PassiveFlags;

/**
 * Created by Fox on 8/17/2015.
 */
public class GlobalFlagSet extends FlagSetBase {

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
    public String getType() {
        return "Global";
    }

    @Override
    public Text getDetails(String[] args) {
        return Texts.of("This is the global FlagSet.");
    }

    @Override
    public FlagState hasPermission(Player player, ActiveFlags flag) {
        return FlagState.TRUE;
    }

    @Override
    public FlagState isFlagAllowed(PassiveFlags flag) {
        return FlagState.TRUE;
    }
}
