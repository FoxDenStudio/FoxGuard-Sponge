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
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.factory.FGFactoryManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class CommandCreate implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    //create region [w:<world>] name type args...
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ", 4);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
                //----------------------------------------------------------------------------------------------------------------------
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
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                if (args[1 + flag].matches("^.*[^0-9a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1 + flag], 1 + flag);
                if (args[1 + flag].matches("^[^a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1 + flag], 1 + flag);
                if (args[1 + flag].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state"))
                    throw new CommandException(Texts.of("You may not use \"" + args[1 + flag] + "\" as a name!"));
                if (args.length < 3 + flag) throw new CommandException(Texts.of("Must specify a type!"));
                IRegion newRegion = FGFactoryManager.getInstance().createRegion(
                        args[1 + flag].toLowerCase(), args[2 + flag],
                        args.length < 4 + flag ? "" : args[3 + flag],
                        FoxGuardCommandDispatcher.getInstance().getStateMap().get(player), world, player);
                boolean success = FoxGuardManager.getInstance().addRegion(world, newRegion);
                if (!success)
                    throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1 + flag], 1 + flag);
                FoxGuardCommandDispatcher.getInstance().getStateMap().get(player).flush(InternalCommandState.StateField.POSITIONS);
                player.sendMessage(Texts.of(TextColors.GREEN, "Region created successfully"));
                return CommandResult.success();
                //----------------------------------------------------------------------------------------------------------------------
            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                if (args[1].matches("^.*[^0-9a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name must be alphanumeric!"), args[1], 1);
                if (args[1].matches("^[^a-zA-Z_$].*$"))
                    throw new ArgumentParseException(Texts.of("Name can't start with a number!"), args[1], 1);
                if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("state"))
                    throw new CommandException(Texts.of("You may not use \"" + args[1] + "\" as a name!"));
                int flag = 0;
                int priority = 0;
                try{
                    priority = Integer.parseInt(args[2]);
                    flag = 1;
                } catch (NumberFormatException ignored){}

                if (args.length < 3 + flag) throw new CommandException(Texts.of("Must specify a type!"));
                IFlagSet newFlagSet = FGFactoryManager.getInstance().createFlagSet(
                        args[1].toLowerCase(), args[2 + flag], priority,
                        args.length < 4 + flag ? "" : args[3 + flag],
                        FoxGuardCommandDispatcher.getInstance().getStateMap().get(player), player);
                boolean success = FoxGuardManager.getInstance().addFlagSet(newFlagSet);
                if (!success)
                    throw new ArgumentParseException(Texts.of("That name is already taken!"), args[1], 1);
                player.sendMessage(Texts.of(TextColors.GREEN, "FlagSet created successfully!"));
                return CommandResult.success();
                //----------------------------------------------------------------------------------------------------------------------
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
        return source.hasPermission("foxguard.command.modify.create");
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
            return Texts.of("create (region [w:<world>] | flagset) <name> <priority> <type> [args...]");
        else return Texts.of("create (region <world> | flagset) <name> <priority> <type> [args...]");
    }
}
