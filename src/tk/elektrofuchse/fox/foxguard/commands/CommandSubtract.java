package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandSubtract implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};
    String[] positionsAliases = {"positions", "position", "points", "point", "locations", "location", "pos", "loc", "locs", "p"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ");
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (FGHelper.contains(regionsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" ", 5);
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name or a number!"));
                IRegion region;
                try {
                    int index = Integer.parseInt(args[1]);
                    region = FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.get(index - 1);
                } catch (NumberFormatException e) {
                    region = FoxGuardManager.getInstance().getRegion(world, args[1]);
                } catch (IndexOutOfBoundsException e) {
                    throw new ArgumentParseException(Texts.of("Index out of bounds! (1 - "
                            + FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.size()), args[1], 1);
                }
                if (region == null)
                    throw new ArgumentParseException(Texts.of("No Regions with this name!"), args[1 + flag], 1 + flag);
                if (!FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.contains(region))
                    throw new ArgumentParseException(Texts.of("Region is not in your state buffer!"), args[1 + flag], 1 + flag);
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedRegions.remove(region);
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully removed Region from your state buffer!"));
                return CommandResult.success();
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name or a number!"));
                IFlagSet flagSet;
                try {
                    int index = Integer.parseInt(args[1]);
                    flagSet = FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.get(index - 1);
                } catch (NumberFormatException e) {
                    flagSet = FoxGuardManager.getInstance().getFlagSet(args[1]);
                } catch (IndexOutOfBoundsException e) {
                    throw new ArgumentParseException(Texts.of("Index out of bounds! (1 - "
                            + FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.size()), args[1], 1);
                }
                if (flagSet == null)
                    throw new ArgumentParseException(Texts.of("No FlagSets with this name!"), args[1], 1);
                if (!FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.contains(flagSet))
                    throw new ArgumentParseException(Texts.of("FlagSet is not in your state buffer!"), args[1], 1);
                FGCommandMainDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.remove(flagSet);
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully removed FlagSet from your state buffer!"));
                return CommandResult.success();
            } else if (FGHelper.contains(positionsAliases, args[0])) {
                try {
                    int index = Integer.parseInt(args[1]);
                    FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions.remove(index - 1);
                } catch (NumberFormatException e) {
                    throw new ArgumentParseException(Texts.of("Index out of bounds! (1 - "
                            + FGCommandMainDispatcher.getInstance().getStateMap().get(player).positions.size()), args[1], 1);
                } catch (IndexOutOfBoundsException e) {
                    throw new ArgumentParseException(Texts.of("Not a valid index!"), args[1], 1);
                }
                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully removed position from your state buffer!"));
                return CommandResult.success();
            } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
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
