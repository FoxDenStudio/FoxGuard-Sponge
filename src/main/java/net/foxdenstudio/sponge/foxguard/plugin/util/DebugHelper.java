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

import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.ChangeBlockEvent;

public class DebugHelper {

    public static void printEvent(Event event) {
        StringBuilder sb = new StringBuilder().append("-----------------------------------\n");
        sb.append(event.getClass()).append("\n\n");
        for (Object o : event.getCause().all()) {
            sb.append(o).append("\n");
        }
        FoxGuardMain.instance().getLogger().info(sb.toString());
    }

    public static void printBlockEvent(ChangeBlockEvent event) {
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
        event.getCause().getNamedCauses().forEach((k, v) -> sb.append(k).append("::").append(v).append("\n"));
        FoxGuardMain.instance().getLogger().info(sb.toString());
    }
}
