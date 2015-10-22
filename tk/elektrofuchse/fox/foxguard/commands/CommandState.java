package tk.elektrofuchse.fox.foxguard.commands;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.source.ConsoleSource;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/20/2015.
 */
public class CommandState implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        TextBuilder output = Texts.builder().append(Texts.of(TextColors.GREEN, "------------------------\n"));
        if (source instanceof Player) {
            Player player = (Player) source;
            int flag = 0;
            if (!FoxGuardCommand.getInstance().getStateMap().get(player).selectedRegions.isEmpty()) {
                output.append(Texts.of(TextColors.GREEN, "Regions: "));
                Iterator<IRegion> regionIterator = FoxGuardCommand.getInstance().getStateMap()
                        .get(player).selectedRegions.iterator();
                while (regionIterator.hasNext()) {
                    IRegion region = regionIterator.next();
                    output.append(Texts.of(getColorForRegion(region), region.getName()));
                    if (regionIterator.hasNext()) output.append(Texts.of(TextColors.RESET, ", "));
                }
                flag++;
            }
            if (!FoxGuardCommand.getInstance().getStateMap().get(player).selectedFlagSets.isEmpty()) {
                if (flag != 0) output.append(Texts.of("\n"));
                output.append(Texts.of(TextColors.GREEN, "FlagSets: "));
                Iterator<IFlagSet> flagSetIterator = FoxGuardCommand.getInstance().getStateMap()
                        .get(player).selectedFlagSets.iterator();
                while (flagSetIterator.hasNext()) {
                    IFlagSet flagSet = flagSetIterator.next();
                    output.append(Texts.of(getColorForFlagSet(flagSet), flagSet.getName()));
                    if (flagSetIterator.hasNext()) output.append(Texts.of(TextColors.RESET, ", "));
                }
                output.append(Texts.of());
                flag++;
            }
            if (!FoxGuardCommand.getInstance().getStateMap().get(player).positions.isEmpty()) {
                if (flag != 0) output.append(Texts.of("\n"));
                output.append(Texts.of(TextColors.GREEN, "Positions:"));
                output.append(Texts.of(TextColors.RESET));
                int index = 1;
                for (Vector3i pos : FoxGuardCommand.getInstance().getStateMap().get(player).positions) {
                    output.append(
                            Texts.of("\nPos" + index + ": (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")")
                    );
                    index++;
                }
                flag++;
            }
            if (flag == 0) player.sendMessage(Texts.of("Your current state buffer is clear!"));
            else player.sendMessage(output.build());


        } else if (source instanceof ConsoleSource) {

        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
        return CommandResult.empty();
    }

    private TextColor getColorForRegion(IRegion region) {
        return region.getName().equals("global") ? TextColors.YELLOW : TextColors.RESET;
    }

    private TextColor getColorForFlagSet(IFlagSet flagSet) {
        return flagSet.getName().equals("global") ? TextColors.YELLOW : TextColors.RESET;
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
        return null;
    }
}
