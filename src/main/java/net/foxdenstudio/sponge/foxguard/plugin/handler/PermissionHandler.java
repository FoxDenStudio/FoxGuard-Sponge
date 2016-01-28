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

package net.foxdenstudio.sponge.foxguard.plugin.handler;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class PermissionHandler extends HandlerBase {

    public PermissionHandler(String name, int priority) {
        super(name, priority);
    }

    @Override
    public EventResult handle(@Nullable User user, Flag flag, Event event) {
        if (user == null) return EventResult.pass();
        while (flag != null) {
            if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".allow"))
                return EventResult.allow();
            else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".deny"))
                return EventResult.deny();
            else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".pass"))
                return EventResult.pass();
            flag = flag.getParent();
        }
        return EventResult.pass();
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
        return Text.builder()
                .append(Text.of(TextColors.GOLD, "Permission:\n"))
                .append(Text.of(TextColors.RESET, "foxguard.handler."))
                .append(Text.of(TextColors.YELLOW, this.getName()))
                .append(Text.of(TextColors.RESET, "."))
                .append(Text.of(TextColors.AQUA, "<flag>"))
                .append(Text.of(TextColors.RESET, "."))
                .append(Text.of(TextColors.AQUA, "<"))
                .append(Text.of(TextColors.GREEN, "allow"))
                .append(Text.of(TextColors.AQUA, "/"))
                .append(Text.of(TextColors.YELLOW, "pass"))
                .append(Text.of(TextColors.AQUA, "/"))
                .append(Text.of(TextColors.RED, "deny"))
                .append(Text.of(TextColors.AQUA, ">")).build();

    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {

    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) {
        return ProcessResult.of(false, Text.of(TextColors.WHITE, "Permission Handlers have no configurable parameters!"));
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

}
