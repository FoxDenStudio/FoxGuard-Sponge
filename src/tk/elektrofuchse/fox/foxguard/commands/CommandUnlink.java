package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.GlobalFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 */
public class CommandUnlink implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ", 2);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0 &&
                        FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions in your state buffer!"));
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any FlagSets in your state buffer!"));
                int[] count = {0};
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.stream().forEach(
                        region -> FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.stream()
                                .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                .forEach(flagSet -> count[0] += FoxGuardManager.getInstance().unlink(region, flagSet) ? 1 : 0));
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                return CommandResult.builder().successCount(count[0]).build();
            } else if (args[0].equals("FULL")) {
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.size() == 0 &&
                        FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size() == 0)
                    throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
                int[] count = {0};
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.stream().forEach(
                        region -> {
                            List<IFlagSet> flagSets = new ArrayList<>();
                            region.getFlagSets().stream()
                                    .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                    .forEach(flagSets::add);
                            flagSets.stream().forEach(flagSet -> count[0] += FoxGuardManager.getInstance().unlink(region, flagSet) ? 1 : 0);
                        });
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.stream()
                        .filter(flagSet -> !(flagSet instanceof GlobalFlagSet)).forEach(
                        flagSet -> FoxGuardManager.getInstance().getRegionsListCopy().stream().forEach(
                                region -> count[0] += FoxGuardManager.getInstance().unlink(region, flagSet) ? 1 : 0));
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                return CommandResult.builder().successCount(count[0]).build();
            } else if (args[0].equals("ALL")) {
                int[] count = {0};
                FoxGuardManager.getInstance().getRegionsListCopy().forEach(
                        region -> {
                            List<IFlagSet> flagSets = new ArrayList<>();
                            region.getFlagSets().stream()
                                    .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                                    .forEach(flagSets::add);
                            flagSets.stream().forEach(flagSet -> count[0] += FoxGuardManager.getInstance().unlink(region, flagSet) ? 1 : 0);
                        });
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully unlinked " + count[0] + "!"));
                return CommandResult.builder().successCount(count[0]).build();
            }
        } else {

        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return false;
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
