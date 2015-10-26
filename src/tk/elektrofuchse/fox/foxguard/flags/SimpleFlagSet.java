package tk.elektrofuchse.fox.foxguard.flags;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.flags.util.PassiveFlags;

/**
 * Created by Fox on 8/17/2015.
 */
public class SimpleFlagSet extends FlagSetBase {

    public SimpleFlagSet(String name, int priority) {
        super(name, priority);
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public FlagState hasPermission(Player player, ActiveFlags flag) {
        if (flag == null) return FlagState.PASSTHROUGH;
        if (flag == ActiveFlags.BLOCK_PLACE && player.hasPermission("foxguard.simple.block.place") ||
                flag == ActiveFlags.BLOCK_BREAK && player.hasPermission("foxguard.simple.block.break")) {
            return FlagState.TRUE;
        } else {
            return FlagState.FALSE;
        }
    }

    @Override
    public FlagState isFlagAllowed(PassiveFlags flag) {
        return FlagState.PASSTHROUGH;
    }

    @Override
    public String getType() {
        return "Simple";
    }

    @Override
    public Text getDetails(String arguments) {
        return Texts.of("This flagset contains no configurable parameters!");
    }

}
