/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/
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

package net.gravityfox.foxguard.flagsets;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.util.ActiveFlags;
import net.gravityfox.foxguard.flagsets.util.PassiveFlags;

import javax.sql.DataSource;

/**
 * Created by Fox on 8/17/2015.
 * Project: foxguard
 */
public class GlobalFlagSet extends FlagSetBase {

    public static final String NAME = "_global";

    public GlobalFlagSet() {
        super(NAME, Integer.MIN_VALUE);
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Integer.MIN_VALUE;
    }

    @Override
    public void setName(String name) {
        this.name = NAME;
    }

    @Override
    public String getType() {
        return "Global";
    }

    @Override
    public String getUniqueType() {
        return "global";
    }

    @Override
    public Text getDetails(String arguments) {
        return Texts.of("It's global. Nothing to see here. Now move along.");
    }

    @Override
    public void writeToDatabase(DataSource dataSource) {

    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public Tristate hasPermission(Player player, ActiveFlags flag, Event event) {
        return Tristate.TRUE;
    }

    @Override
    public Tristate isFlagAllowed(PassiveFlags flag, Event event) {
        return Tristate.TRUE;
    }
}
