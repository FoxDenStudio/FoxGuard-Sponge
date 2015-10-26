package tk.elektrofuchse.fox.foxguard.commands;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;

import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Created by Fox on 10/25/2015.
 */
public class CommandAbout implements CommandCallable {
    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        TextBuilder builder = Texts.builder();
        builder.append(Texts.of(TextColors.GOLD, "FoxGuard World Protection Plugin\n"));
        builder.append(Texts.of("Version: " + FoxGuardMain.PLUGIN_VERSION + "\n"));
        builder.append(Texts.of("Author: gravityfox"));
        source.sendMessage(builder.build());
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
        return Optional.of(Texts.of("Why would you need help using the \"about\" command?"));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Texts.of("about");
    }
}
