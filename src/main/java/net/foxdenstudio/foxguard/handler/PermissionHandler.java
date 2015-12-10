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

package net.foxdenstudio.foxguard.handler;

import net.foxdenstudio.foxcore.commands.util.ProcessResult;
import net.foxdenstudio.foxcore.commands.util.SourceState;
import net.foxdenstudio.foxguard.handler.util.Flags;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;

public class PermissionHandler extends HandlerBase {

    public PermissionHandler(String name, int priority) {
        super(name, priority);
    }

    @Override
    public Tristate handle(@Nullable User user, Flags flag, Event event) {
        try {
            this.lock.readLock().lock();
            if (!isEnabled || user == null) return Tristate.UNDEFINED;
            this.lock.readLock().unlock();
            while (flag != null) {
                if (user.hasPermission("foxguard.flags." + this.name + "." + flag.flagName() + ".allow"))
                    return Tristate.TRUE;
                else if (user.hasPermission("foxguard.flags." + this.name + "." + flag.flagName() + ".deny"))
                    return Tristate.FALSE;
                flag = flag.getParent();
            }
            return Tristate.UNDEFINED;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public String getShortTypeName() {
        return "Perm";
    }

    @Override
    public String getLongTypeName() {
        return "Permission";
    }

    @Override
    public String getUniqueTypeString() {
        return "permission";
    }

    @Override
    public Text getDetails(String arguments) {
        return Texts.builder()
                .append(Texts.of(TextColors.GOLD, "Permission: "))
                .append(Texts.of(TextColors.RESET, "foxguard.flags."))
                .append(Texts.of(TextColors.YELLOW, this.getName()))
                .append(Texts.of(TextColors.RESET, "."))
                .append(Texts.of(TextColors.AQUA, "<flagname>"))
                .append(Texts.of(TextColors.RESET, "."))
                .append(Texts.of(TextColors.AQUA, "<"))
                .append(Texts.of(TextColors.GREEN, "allow"))
                .append(Texts.of(TextColors.AQUA, "/"))
                .append(Texts.of(TextColors.RED, "deny"))
                .append(Texts.of(TextColors.AQUA, ">")).build();

    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {

    }

    @Override
    public ProcessResult modify(String arguments, SourceState state, CommandSource source) {
        return ProcessResult.of(false, Texts.of(TextColors.WHITE, "Permission Handlers have no configurable parameters!"));
    }

}
