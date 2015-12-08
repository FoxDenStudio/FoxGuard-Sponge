/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.foxguard;

import net.foxdenstudio.foxcommon.commands.util.ProcessResult;
import net.foxdenstudio.foxcommon.commands.util.SourceState;
import net.foxdenstudio.foxguard.commands.CommandDetail;
import net.foxdenstudio.foxguard.handlers.IHandler;
import net.foxdenstudio.foxguard.regions.IRegion;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Interface for all FoxGuard Objects. Inherited by {@link IRegion Regions}
 * and {@link IHandler Handlers}.
 * Essentially the core of the code, this is the most used interface.
 */
public interface IFGObject {

    /**
     * Gets the name of the object. It should be alphanumeric with limited use of special characters.
     *
     * @return Name of object.
     */
    String getName();

    /**
     * Sets the name of the object. It should be alphanumeric with limited use of special characters.
     *
     * @param name The new name of the object.
     */
    void setName(String name);

    /**
     * Gets the short type name for this object. It should be around four letters. It should be human readable.
     * It is used in object lists, such as when using the list or detail commands.
     *
     * @return The human readable short type name.
     */
    String getShortTypeName();

    /**
     * Gets the long type name for this object. It should be human readable. Avoid abbreviating.
     * It is used in the general information section of the detail command.
     *
     * @return The human readable long type name.
     */
    String getLongTypeName();

    /**
     * Gets the unique ID for this object. It should be alphabetic only. This return must be static.
     * This is used for SQL storage. It is the same identifier used in the object factories. This return should be static.
     *
     * @return The unique identifier for this type of object.
     */
    String getUniqueTypeString();

    /**
     * Returns whether this object is enabled or disabled.
     *
     * @return Enable status.
     */
    boolean isEnabled();

    /**
     * Sets the enable status of this object.
     * It is up to the object to behave accordingly with its enable status.
     * Disabled Regions should return false when queried and disabled Handlers should return {@link org.spongepowered.api.util.Tristate#UNDEFINED Tristate.UNDEFINED}
     *
     * @param state
     */
    void setIsEnabled(boolean state);

    /**
     * Gets the details for the object as a SpongeAPI {@link Text} Object. Used in the {@link CommandDetail Detail} command.
     * Should be dynamically generated with formatted text. Multiple lines are allowed.
     * Any arguments leftover from the detail command are passed to the detail method.
     * This allows specific queries in case there is more data stored than can reasonable displayed.
     * It is recommended to have click action wherever possible to ease the configuration of objects.
     *
     * @param arguments The extra arguments from the {@link CommandDetail Detail} command. Object should still return something meaningful if this is empty.
     * @return A {@link Text} object that provides meaningful information about the object.
     */
    Text getDetails(String arguments);

    /**
     * Called when saving objects to a database. A datasource is given, which can be turned into a connection.
     * This is an empty database if nothing has been saved previously, otherwise it is the database as it was the previous save.
     * This method should fail gracefully from database inconsistencies. Throwing an {@link SQLException} will cause the metadata for the object to not be written.
     * Subsequently the
     *
     * @param dataSource The datasource for the database specific to this object.
     * @throws SQLException Thrown due to database errors.
     */
    void writeToDatabase(DataSource dataSource) throws SQLException;

    /**
     * Specifies whether FoxGuard should attempt to save this object into SQL.
     * Set to false if a separate storage mechanism will be used. Defaults to true.
     * Objects that return false must be responsible for storing and loading ALL data.
     * The only time this should be false is if another plugin is hooking into FoxGuard
     * using a delegate object. In that case the object is just a transient API accessor and should not be saved.
     *
     * @return Whether FoxGuard should autosave this object.
     */
    default boolean autoSave() {
        return true;
    }

    ProcessResult modify(String arguments, SourceState state, CommandSource source);

}
