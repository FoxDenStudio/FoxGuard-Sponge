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

package net.foxdenstudio.sponge.foxguard.plugin.util;

import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.text.Text;

import java.util.Map;

public class DebugManager extends FCCommandBase {

    public static final DebugManager INSTANCE = new DebugManager();

    private boolean enabled = false;
    private int armCount = 0;

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] parts = arguments.split(" +");
        if (parts.length == 0) {
            source.sendMessage(getUsage(source));
            return CommandResult.empty();
        }

        switch (parts[0]) {
            case "on":
                enabled = true;
                armCount = 0;
                source.sendMessage(Text.of("Enabled printing"));
                break;
            case "off":
                enabled = false;
                armCount = 0;
                source.sendMessage(Text.of("Disabled printing"));
            case "arm":
                enabled = true;
                int count = 1;
                if (parts.length > 1) {
                    try {
                        count = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                armCount = count;
                source.sendMessage(Text.of("Armed for " + count + " prints"));
            default:
                return CommandResult.empty();
        }
        return CommandResult.empty();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.debug");
    }

    private boolean print() {
        boolean ret = enabled;
        if (armCount > 0) {
            if (--armCount == 0) enabled = false;
        }
        return ret;
    }

    public void printEvent(Event event) {
        if (print()) {
            StringBuilder sb = new StringBuilder().append("-----------------------------------\n");
            sb.append(event.getClass()).append("\n\n");

            for (Object o : event.getCause().all()) {
                sb.append(o).append("\n");
            }

            sb.append("\n");

            for (Map.Entry<EventContextKey<?>, Object> entry : event.getContext().asMap().entrySet()) {
                sb.append(entry).append("\n");
            }
            FoxGuardMain.instance().getLogger().info(sb.toString());
        }
    }

    public void printBlockEvent(ChangeBlockEvent event) {
        if (print()) {
            StringBuilder sb = new StringBuilder().append("-----------------------------------\n");
            sb.append(event.getClass()).append("\n");
            for (Transaction t : event.getTransactions()) {
                sb.append(t).append("\n");
            }
            sb.append("\n");
            for (Object o : event.getCause().all()) {
                sb.append(o).append("\n");
            }
            sb.append("\n");
            event.getContext().asMap().forEach((k, v) -> sb.append(k).append("::").append(v).append("\n"));
            FoxGuardMain.instance().getLogger().info(sb.toString());
        }
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("debug <on | off | arm [count]>");
    }
}
