package tk.elektrofuchse.fox.foxguard.commands;

import com.google.common.base.Optional;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/18/2015.
 */
public class CommandList implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!(source instanceof Player) || !(source instanceof ConsoleSource))
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));

        String[] args = arguments.split(" ");
        int page = 1;
        if (args.length > 0) {
            int pageFlag = 0;
            try {
                page = Integer.parseInt(args[0]);
                pageFlag = 1;
            } catch (NumberFormatException ignored) {
            }
            if (args.length > pageFlag) {
                if (contains(regionsAliases, args[1])) {
                    List<String> regionList = new LinkedList<>();
                    if (args.length > pageFlag + 1) {
                        Optional<World> optWorld = FoxGuardManager.getInstance().getServer().getWorld(args[pageFlag + 1]);
                        if (!optWorld.isPresent())
                            throw new ArgumentParseException(Texts.of("No world found with that name!"), args[pageFlag + 1], pageFlag + 1);
                        FoxGuardManager.getInstance().getRegionsListCopy(optWorld.get()).forEach(region -> {
                            regionList.add(region.getWorld().getName() + ":" + region.getName());
                        });
                    } else {
                        FoxGuardManager.getInstance().getRegionsListCopy().forEach(region -> {
                            regionList.add(region.getWorld().getName() + ":" + region.getName());
                        });
                    }
                    TextBuilder output = Texts.builder();
                    regionList.forEach(region -> output.append(Texts.of(region)));
                    source.sendMessage(output.build());
                }
            }
        }

        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.list");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Texts.of("Lists the regions/flagsets in this world/server"));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.absent();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("list [page] (regions [world])|flagsets");
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    public enum ItemType {
        REGIONS,
        FLAGLISTS,
        BOTH;
    }
}
