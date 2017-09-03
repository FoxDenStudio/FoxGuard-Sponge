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

package net.foxdenstudio.sponge.foxguard.plugin.object;

import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.IModifiable;
import net.foxdenstudio.sponge.foxguard.plugin.command.CommandDetail;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import net.foxdenstudio.sponge.foxguard.plugin.storage.FGStorageManagerNew;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Interface for all FoxGuard Objects. Inherited by {@link IRegion Regions}, {@link IWorldRegion World Regions},
 * and {@link IHandler Handlers}.
 * Essentially the core of the code, this is the most used interface.
 */
public interface IFGObject extends IModifiable {

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

    UUID getOwner();

    void setOwner(UUID owner);

    /**
     * Gets the short direction name for this object. It should be around four letters. It should be human readable.
     * It is used in object lists, such as when using the list or detail commands.
     *
     * @return The human readable short direction name.
     */
    String getShortTypeName();

    /**
     * Gets the long direction name for this object. It should be human readable. Avoid abbreviating.
     * It is used in the general information section of the detail command.
     *
     * @return The human readable long direction name.
     */
    String getLongTypeName();

    /**
     * Gets the unique ID for this object. It should be alphabetic only. This return must be static.
     * This is used for SQL storage. It is the same identifier used in the object factories. This return should be static.
     *
     * @return The unique identifier for this direction of object.
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
     *
     * @param state
     */
    void setEnabled(boolean state);

    /**
     * Gets the details for the object as a SpongeAPI {@link Text} Object. Used in the {@link CommandDetail Detail} command.
     * Should be dynamically generated with formatted text. Multiple lines are allowed.
     * Any arguments leftover from the detail command are passed to the detail method.
     * This allows specific queries in case there is more data stored than can reasonably displayed.
     * It is recommended to have click actions wherever possible to ease the configuration of objects.
     *
     * @param source
     * @param arguments The extra arguments from the {@link CommandDetail Detail} command. Object should still return something meaningful if this is empty.
     * @return A {@link Text} object that provides meaningful information about the object.
     */
    Text details(CommandSource source, String arguments);

    List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition);

    /**
     * Called when the object is being saved.
     * It is completely up to the object how it wants to save itself.
     * The only requirement is that it stays within its own directory and is able to load itself later.
     *
     * @param directory The directory for this object to save to.
     */
    void save(Path directory);

    /**
     * Specifies whether FoxGuard should try saving this object.
     * This simply involves saving the object to a list, creating a metadata file,
     * and then calling {@link IFGObject#save(Path)}
     * <p>
     * This should be set to false only when an object already employs some other method of persistence, or simply does not require it.
     * This will often be the case if another plugin is hooking into FoxGuard and using an object as a kind of API accessor.
     * These object are either transient, or have their own methods of persistence.
     * <p>
     * When set to false, FoxGuard will save this object, meaning it will not be loaded on the next server start,
     * which means the object must be re-added by some other means, or not at all.
     *
     * @return Whether FoxGuard should auto-save this object.
     */
    @SuppressWarnings("SameReturnValue")
    default boolean autoSave() {
        return true;
    }

    /**
     * This method is called when the modify command is used to try to modify an object.
     * This method is essentially a command handler that does a specific job.
     * <p>
     * The only really important thing to note is that it is (REALLY) important that an object report a modify result of success if and only if the object was actually modified.
     * "Actually modified" implies that the object needs to be saved again.
     * <p>
     * This contract is less important if an object implements the {@link IFGObject#shouldSave()} method.
     *
     * @param source    The {@link CommandSource} of the modify command
     * @param arguments The {@link String} arguments specifically for this object
     * @return the result of the operation. The success flag should be true if and only if the object was changed in some way.
     * @throws CommandException If there is an issue parsing the command.
     */

    @Override
    ProcessResult modify(CommandSource source, String arguments) throws CommandException;

    default boolean shouldSave() {
        return FGStorageManagerNew.getInstance().defaultModifiedMap.get(this);
    }

}
