package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandParseHelper;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 8/22/2015.
 */
public class CommandDetail implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    //fg detail <region> [w:<world>] <name>

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
                IRegion region = FoxGuardManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new CommandException(Texts.of("No region with name \"" + args[1 + flag] + "\"!"));

                player.sendMessage(region.getDetails(Arrays.copyOfRange(args, 2 + flag, args.length)));

            } else if (CommandParseHelper.contains(flagSetsAliases, args[0])) {

            } else {
                throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
            }
        } else if (source instanceof ConsoleSource) {

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
        return null;
    }
}
