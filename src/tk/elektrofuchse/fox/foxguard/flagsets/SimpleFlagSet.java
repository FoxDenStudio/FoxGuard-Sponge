package tk.elektrofuchse.fox.foxguard.flagsets;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flagsets.util.PassiveFlags;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
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
    public Tristate hasPermission(Player player, ActiveFlags flag) {
        if (flag == null) return Tristate.UNDEFINED;
        if (flag == ActiveFlags.BLOCK_PLACE) {
            if (player.hasPermission("foxguard.flags.simple.block.place"))
                return Tristate.TRUE;
            else return Tristate.FALSE;
        }
        if (flag == ActiveFlags.BLOCK_BREAK) {
            if (player.hasPermission("foxguard.flags.simple.block.break"))
                return Tristate.TRUE;
            else return Tristate.FALSE;
        }
        return Tristate.UNDEFINED;
    }

    @Override
    public Tristate isFlagAllowed(PassiveFlags flag) {
        return Tristate.UNDEFINED;
    }

    @Override
    public String getType() {
        return "Simple";
    }

    @Override
    public String getUniqueType() {
        return "simple";
    }

    @Override
    public Text getDetails(String arguments) {
        return Texts.of("This flagset contains no configurable parameters!");
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {

    }

}
