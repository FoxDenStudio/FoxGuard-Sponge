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
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class PermissionHandler extends HandlerBase {

    public PermissionHandler(String name, int priority) {
        super(name, priority);
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Event event) {
        if (user == null) return EventResult.pass();
        /*while (flag != null) {
            if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".allow"))
                return EventResult.allow();
            else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".deny"))
                return EventResult.deny();
            else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." + flag.flagName() + ".pass"))
                return EventResult.pass();
            flag = flag.getParents().length > 0 ? flag.getParents()[0] : null;
        }*/

        for (Set<IFlag> level : flag.getHierarchy()) {
            for (IFlag f : level) {
                if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." +
                        f.flagName() + ".allow"))
                    return EventResult.allow();
                else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." +
                        f.flagName() + ".deny"))
                    return EventResult.deny();
                else if (user.hasPermission("foxguard.handler." + this.name.toLowerCase() + "." +
                        f.flagName() + ".pass"))
                    return EventResult.pass();
            }
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
    public Text details(CommandSource source, String arguments) {
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
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {

    }


    @Override
    public ProcessResult modify(CommandSource source, String arguments) {
        return ProcessResult.of(false, Text.of(TextColors.WHITE, "PermissionHandlers have no configurable parameters!"));
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    public static class Factory implements IHandlerFactory {

        private final String[] permissionAliases = {"permission", "permissions", "perm", "perms"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) {
            return new PermissionHandler(name, priority);
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            PermissionHandler handler = new PermissionHandler(name, priority);
            handler.setIsEnabled(isEnabled);
            return handler;
        }

        @Override
        public String[] getAliases() {
            return permissionAliases;
        }

        @Override
        public String getType() {
            return "permission";
        }

        @Override
        public String getPrimaryAlias() {
            return "permission";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }

}
