package tk.elektrofuchse.fox.foxguard.commands;

import com.google.common.base.Optional;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandParseHelper;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;
import tk.elektrofuchse.fox.foxguard.regions.RectRegion;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 */
public class CommandCreate implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    String[] rectAliases = {"rectangular", "rectangle", "rect"};

    //create region [w:<world>] name type args...


    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ");
        if (source instanceof Player) {
            Player player = (Player) source;
            if (CommandParseHelper.contains(regionsAliases, args[0])) {
                int flag = 0;
                Optional<World> optWorld = CommandParseHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                } else {
                    world = player.getWorld();
                }
                if (args.length < 1 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                if (args[1 + flag].matches("^[^a-zA-Z].*"))
                    throw new CommandException(Texts.of("Name must start with a letter!"));
                if (args[1 + flag].matches("^.*[^0-9a-zA-Z].*$"))
                    throw new CommandException(Texts.of("Name must be alphanumeric!"));
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a type!"));
                IRegion newRegion;
                if (CommandParseHelper.contains(rectAliases, args[2 + flag])) {
                    newRegion = new RectRegion(args[1 + flag].toLowerCase(),
                            FoxGuardCommand.getInstance().getStateMap().get(player).positions,
                            Arrays.copyOfRange(args, 3 + flag, args.length), player);
                } else {
                    throw new ArgumentParseException(Texts.of("Not a valid type!"), args[2 + flag], 2 + flag);
                }
                FoxGuardManager.getInstance().addRegion(world, newRegion);
                FoxGuardCommand.getInstance().getStateMap().get(player).flush(CommandState.StateField.POSITIONS);
                player.sendMessage(Texts.of("Region created successfully"));

            } else if (CommandParseHelper.contains(flagSetsAliases, args[0])) {

            } else {
                throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
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
        return source.hasPermission("foxguard.modify.create");
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
