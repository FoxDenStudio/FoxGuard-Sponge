package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import java.nio.file.Path;

/**
 * Created by Fox on 3/27/2016.
 */
public interface IControllerFactory extends IHandlerFactory {

    @Override
    IController create(String name, int priority, String arguments, CommandSource source) throws CommandException;

    @Override
    IController create(Path directory, String name, int priority, boolean isEnabled);
}
