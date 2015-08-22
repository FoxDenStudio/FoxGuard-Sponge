package tk.elektrofuchse.fox.foxguard.commands;

import com.google.common.base.Optional;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.*;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandState;

import java.util.List;

/**
 * Created by Fox on 8/20/2015.
 */
public class CommandFlush implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};
    String[] positionsAliases = {"positions", "position", "points", "point", "locations", "location", "pos", "loc", "p"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (source instanceof Player) {
            Player player = (Player) source;
            String[] args;
            if (arguments.isEmpty()) {
                FoxGuardCommand.getInstance().getStateMap().get(player).flush();
            } else {
                args = arguments.split(" ");
                for (String arg : args) {
                    CommandState.StateField type = getType(arg);
                    if (type == null) throw new CommandException(Texts.of("\"" + arg + "\" is not a valid type!"));
                    FoxGuardCommand.getInstance().getStateMap().get(player).flush(type);
                }
            }
            player.sendMessage(Texts.of("Successfully flushed!"));
        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
        return CommandResult.empty();
    }

    public CommandState.StateField getType(String input) {
        if (contains(regionsAliases, input)) return CommandState.StateField.REGIONS;
        else if (contains(flagSetsAliases, input)) return CommandState.StateField.FLAGSETS;
        else if (contains(positionsAliases, input)) return CommandState.StateField.POSITIONS;
        else return null;
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return null;
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return null;
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("flush [regions] [flagsets] [positions]");
    }
}
