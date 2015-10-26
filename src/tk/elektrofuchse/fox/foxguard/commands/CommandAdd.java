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
import tk.elektrofuchse.fox.foxguard.commands.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 */
public class CommandAdd implements CommandCallable {

    String[] regionsAliases = {"region", "reg", "r"};
    String[] flagSetsAliases = {"flagset", "flag", "f"};

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
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FoxGuardManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new ArgumentParseException(Texts.of("No Regions with this name!"), args[1 + flag], 1 + flag);
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.contains(region))
                    throw new ArgumentParseException(Texts.of("Region is already in your state buffer!"), args[1 + flag], 1 + flag);
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedRegions.add(region);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added Region to your state buffer!"));
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                IFlagSet flagSet = FoxGuardManager.getInstance().getFlagSet(args[1]);
                if (flagSet == null)
                    throw new ArgumentParseException(Texts.of("No FlagSets with this name!"), args[1], 1);
                if (FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.contains(flagSet))
                    throw new ArgumentParseException(Texts.of("FlagSet is already in your state buffer!"), args[1], 1);
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).selectedFlagSets.add(flagSet);

                source.sendMessage(Texts.of(TextColors.GREEN, "Successfully added FlagSet to your state buffer!"));
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
        return true;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        if (source instanceof Player)
            return Texts.of("detail (region [w:<worldname>] | flagset) <name>");
        else return Texts.of("detail (region <worldname> | flagset) <name>");
    }
}
