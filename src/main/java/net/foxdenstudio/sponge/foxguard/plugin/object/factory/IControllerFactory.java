package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import org.spongepowered.api.command.CommandSource;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Fox on 3/27/2016.
 */
public interface IControllerFactory extends IHandlerFactory {

    @Override
    IController create(String name, int priority, String arguments, CommandSource source);

    @Override
    IController create(DataSource source, String name, int priority, boolean isEnabled) throws SQLException;
}
