package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.*;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Created by Fox on 8/18/2015.
 */
public class CommandList implements CommandCallable {

    String[] regionsAliases = {"regions", "region", "reg", "r"};
    String[] flagSetsAliases = {"flagsets", "flagset", "flags", "flag", "f"};

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (source instanceof Player || (source instanceof ConsoleSource)) {

            String[] args = {};
            if (!arguments.isEmpty()) args = arguments.split(" ");
            int page = 1;
            if (args.length > 0) {
                int pageFlag = 0;
                try {
                    page = Integer.parseInt(args[0]);
                    pageFlag = 1;
                } catch (NumberFormatException ignored) {
                }
                if (args.length > pageFlag) {
                    if (contains(regionsAliases, args[pageFlag])) {
                        List<IRegion> regionList = new LinkedList<>();
                        boolean allFlag;
                        String worldName = "";
                        if (args.length > pageFlag + 1) {
                            Optional<World> optWorld = FoxGuardManager.getInstance().getServer().
                                    getWorld(args[pageFlag + 1]);
                            if (!optWorld.isPresent())
                                throw new ArgumentParseException(Texts.of("No world found with that name!"),
                                        args[pageFlag + 1], pageFlag + 1);
                            FoxGuardManager.getInstance().getRegionsListCopy(optWorld.get()).forEach(regionList::add);
                            allFlag = false;
                            worldName = optWorld.get().getName();
                        } else {
                            FoxGuardManager.getInstance().getRegionsListCopy().forEach(regionList::add);
                            allFlag = true;
                        }
                        TextBuilder output = Texts.builder(
                                "---Regions" + (allFlag ? "" : (" for world: \"" + worldName + "\"")) + "---\n")
                                .color(TextColors.GREEN);
                        ListIterator<IRegion> regionListIterator = regionList.listIterator();
                        while (regionListIterator.hasNext()) {
                            IRegion region = regionListIterator.next();
                            output.append(Texts.of(getColorForRegion(region), getRegionName(region, allFlag)));
                            if (regionListIterator.hasNext()) output.append(Texts.of("\n"));
                        }
                        source.sendMessage(output.build());
                    } else if (contains(flagSetsAliases, args[pageFlag])) {
                        List<IFlagSet> flagSetList = FoxGuardManager.getInstance().getFlagSetsListCopy();
                        TextBuilder output = Texts.builder("---FlagSets---\n").color(TextColors.GREEN);
                        ListIterator<IFlagSet> flagSetListIterator = flagSetList.listIterator();
                        while (flagSetListIterator.hasNext()) {
                            IFlagSet flagSet = flagSetListIterator.next();
                            output.append(Texts.of(getColorForFlagSet(flagSet), flagSet.getName()));
                            if (flagSetListIterator.hasNext()) output.append(Texts.of("\n"));
                        }
                        source.sendMessage(output.build());
                    } else {
                        throw new ArgumentParseException(Texts.of("Not a valid category!"), args[pageFlag], pageFlag);
                    }
                } else {
                    throw new CommandException(Texts.of("Must specify a category!"));
                }
            } else {
                throw new CommandException(Texts.of("Must specify a category!"));
            }

            return CommandResult.empty();
        } else {
            throw new CommandPermissionException(Texts.of("You must be a player or console to use this command!"));
        }
    }

    private TextColor getColorForRegion(IRegion region) {
        return region.getName().equals("global") ? TextColors.YELLOW : TextColors.RESET;
    }

    private TextColor getColorForFlagSet(IFlagSet flagSet) {
        return flagSet.getName().equals("global") ? TextColors.YELLOW : TextColors.RESET;
    }

    private String getRegionName(IRegion region, boolean dispWorld) {
        return (dispWorld ? region.getWorld().getName() + " : " : "") + region.getName();
    }

    private boolean contains(String[] aliases, String input) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.info.list");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Texts.of("Lists the regions/flagsets on this server"));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("list [page] <(regions [world]) | flagsets>");
    }
}
