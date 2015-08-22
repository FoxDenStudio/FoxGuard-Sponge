package tk.elektrofuchse.fox.foxguard.commands;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Optional;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandParseHelper;

import java.util.List;

/**
 * Created by Fox on 8/20/2015.
 */
public class CommandPosition implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (source instanceof Player) {
            Player player = (Player) source;
            String[] args = {};
            if (!arguments.isEmpty()) args = arguments.split(" ");
            int x, y, z;
            Vector3i pPos = player.getLocation().getBlockPosition();
            if (args.length == 0) {
                x = pPos.getX();
                y = pPos.getY();
                z = pPos.getZ();
            } else if (args.length > 0 && args.length < 3) {
                throw new CommandException(Texts.of("Not enough arguments!"));
            } else if (args.length == 3) {
                try {
                    x = CommandParseHelper.parseCoordinate(pPos.getX(), args[0]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[0] + "\"!"), e, args[0], 0);
                }
                try {
                    y = CommandParseHelper.parseCoordinate(pPos.getY(), args[1]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[1] + "\"!"), e, args[1], 1);
                }
                try {
                    z = CommandParseHelper.parseCoordinate(pPos.getZ(), args[2]);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Unable to parse \"" + args[2] + "\"!"), e, args[2], 2);
                }
            } else {
                throw new CommandException(Texts.of("Too many arguments!"));
            }
            FoxGuardCommand.getInstance().getStateMap().get(player).positions.add(new Vector3i(x, y, z));
            player.sendMessage(Texts.of("Successfully added position (" + x + ", " + y + ", " + z + ") to the stack!"));
        } else if (source instanceof ConsoleSource) {

        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
        return CommandResult.empty();
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
        return Texts.of("position [<x> <y> <z>]");
    }
}
