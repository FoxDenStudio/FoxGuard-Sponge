package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.commands.util.FGHelper;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
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
        if (!arguments.isEmpty()) args = arguments.split(" ", 3);
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
                    args = arguments.split(" ", 4);
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FoxGuardManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new CommandException(Texts.of("No region with name \"" + args[1 + flag] + "\"!"));
                TextBuilder builder = Texts.builder();
                builder.append(Texts.of(TextColors.GREEN, "---Details---\n"));
                builder.append(region.getDetails(args.length < 3 + flag ? "" : args[2+flag]));
                builder.append(Texts.of(TextColors.GREEN, "\n---Linked FlagSets---"));
                if(region.getFlagSets().size() == 0) builder.append(Texts.of(TextStyles.ITALIC, "\nNo linked FlagSets!"));
                region.getFlagSets().stream().forEach(flagSet -> builder.append(Texts.of(FGHelper.getColorForFlagSet(flagSet),
                        "\n" + flagSet.getType() + " : " + flagSet.getName())));
                player.sendMessage(builder.build());

            } else if (FGHelper.contains(flagSetsAliases, args[0])) {
                if (args.length < 1) throw new CommandException(Texts.of("Must specify a name!"));
                IFlagSet flagSet = FoxGuardManager.getInstance().getFlagSet(args[1]);
                if (flagSet == null)
                    throw new CommandException(Texts.of("No region with name \"" + args[1] + "\"!"));

                player.sendMessage(flagSet.getDetails(args.length < 3 ? "" : args[2]));
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

        return source.hasPermission("foxguard.command.info.detail");
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
            return Texts.of("detail (region [w:<worldname>] | flagset) <name> [args...]");
        else if (source instanceof ConsoleSource)
            return Texts.of("detail (region <worldname> | flagset) <name> [args...]");
        else return Texts.of("You can't run this command!");
    }
}
