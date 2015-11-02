/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.command.CommandSource;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Fox on 10/28/2015.
 * Project: foxguard
 */
public interface IFGObject {

    String getName();

    void setName(String name);

    String getType();

    String getUniqueType();

    Text getDetails(String arguments);

    void writeToDatabase(DataSource dataSource) throws SQLException;

    boolean modify(String arguments, InternalCommandState state, CommandSource source);

}
