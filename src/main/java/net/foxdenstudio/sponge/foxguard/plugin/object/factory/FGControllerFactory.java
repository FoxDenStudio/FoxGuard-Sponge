package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxguard.plugin.controller.MessageController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class FGControllerFactory implements IHandlerFactory {

    private final String[] messageAliases = {"message", "mess", "msg"};
    private final String[] types = {"message"};

    @Override
    public String[] getAliases() {
        return FCHelper.concatAll(messageAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }

    @Override
    public String[] getPrimaryAliases() {
        return types;
    }

    @Override
    public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public IHandler create(String name, String type, int priority, String arguments, CommandSource source) {
        if (Aliases.isIn(messageAliases, type)) {
            MessageController handler = new MessageController(name, priority);
            return handler;
        } else return null;
    }

    @Override
    public IHandler create(DataSource source, String name, String type, int priority, boolean isEnabled) throws SQLException {
        return null;
    }
}
