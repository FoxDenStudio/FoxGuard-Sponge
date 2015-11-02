/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ConsoleSource;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.GlobalFlagSet;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class CommandLink implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" ", 2);
        if (args.length == 0) {
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.size() == 0 &&
                    FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions or FlagSets in your state buffer!"));
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.size() == 0)
                throw new CommandException(Texts.of("You don't have any Regions in your state buffer!"));
            if (FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.size() == 0)
                throw new CommandException(Texts.of("You don't have any FlagSets in your state buffer!"));
            int[] count = {0};
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedRegions.stream().forEach(
                    region -> FGCommandMainDispatcher.getInstance().getStateMap().get(source).selectedFlagSets.stream()
                            .filter(flagSet -> !(flagSet instanceof GlobalFlagSet))
                            .forEach(flagSet -> count[0] += FoxGuardManager.getInstance().link(region, flagSet) ? 1 : 0));
            source.sendMessage(Texts.of(TextColors.GREEN, "Successfully linked " + count[0] + "!"));
            FGCommandMainDispatcher.getInstance().getStateMap().get(source).flush(InternalCommandState.StateField.REGIONS, InternalCommandState.StateField.FLAGSETS);
            return CommandResult.builder().successCount(count[0]).build();
        } else {
            if (source instanceof Player) {
                Player player = (Player) source;
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[0], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" ", 3);
                } else world = player.getWorld();
                if (args.length < 1 + flag) throw new CommandException(Texts.of("Must specify items to link!"));
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a flagset!"));
                boolean success = FoxGuardManager.getInstance().link(world, args[flag], args[1 + flag]);
            } else {

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
        return false;
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
            return Texts.of("link [ [w:<worldname>] <region name> <flagset name> ]");
        else {
            return Texts.of("link [ <worldname> <region name> <flagset name> ]");
        }
    }
}
