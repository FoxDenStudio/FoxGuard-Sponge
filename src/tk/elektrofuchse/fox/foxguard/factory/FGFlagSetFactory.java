/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.factory;

import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;
import tk.elektrofuchse.fox.foxguard.flagsets.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flagsets.SimpleFlagSet;
import tk.elektrofuchse.fox.foxguard.util.FGHelper;

import javax.sql.DataSource;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGFlagSetFactory implements IFlagSetFactory {

    String[] simpleAliases = {"simple"};
    String[] types = {"simple"};

    @Override
    public IFlagSet createFlagSet(String name, String type, int priority, String arguments, InternalCommandState state, CommandSource source) {
        if (type.equalsIgnoreCase("simple")) {

            return new SimpleFlagSet(name, priority);
        } else return null;
    }

    @Override
    public IFlagSet createFlagSet(DataSource source, String name, String type, int priority) {
        if (type.equalsIgnoreCase("simple")) {

            return new SimpleFlagSet(name, priority);
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(simpleAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}
