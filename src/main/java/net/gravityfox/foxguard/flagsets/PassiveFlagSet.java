/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.util.Flags;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.source.ProxySource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.*;

/**
 * Created by Fox on 11/26/2015.
 * Project: SpongeForge
 */
public class PassiveFlagSet extends OwnableFlagSetBase {


    public PassiveFlagSet(String name, int priority) {
        super(name, priority);
    }

    @Override
    public Tristate isAllowed(@Nullable User user, Flags flag, Event event) {
        if (!isEnabled) return Tristate.UNDEFINED;

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        if (!source.hasPermission("foxguard.command.modify.objects.modify.flagsets")) {
            if (source instanceof ProxySource) source = ((ProxySource) source).getOriginalSource();
            if (source instanceof Player && !this.ownerList.contains(source)) return false;
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (args.length > 0) {
            if (isAlias(ownerAliases, args[1])) {
                if (args.length > 1) {
                    UserOperations op;
                    if (args[2].equalsIgnoreCase("add")) {
                        op = UserOperations.ADD;
                    } else if (args[2].equalsIgnoreCase("remove")) {
                        op = UserOperations.REMOVE;
                    } else if (args[2].equalsIgnoreCase("set")) {
                        op = UserOperations.SET;
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Not a valid operation!"));
                        return false;
                    }
                    if (args.length > 2) {
                        int successes = 0;
                        int failures = 0;
                        List<String> names = new ArrayList<>();
                        for (String name : Arrays.copyOfRange(args, 3, args.length)) {
                            names.add(name);
                        }
                        List<User> users = new ArrayList<>();
                        for (String name : names) {
                            Optional<User> optUser = FoxGuardMain.getInstance().getUserStorage().get(name);
                            if (optUser.isPresent() && !FGHelper.isUserOnList(users, optUser.get()))
                                users.add(optUser.get());
                            else failures++;
                        }
                        switch (op) {
                            case ADD:
                                for (User user : users) {
                                    if (!FGHelper.isUserOnList(ownerList, user) && ownerList.add(user))
                                        successes++;
                                    else failures++;
                                }
                                break;
                            case REMOVE:
                                for (User cUser : ownerList) {
                                    if (FGHelper.isUserOnList(users, cUser)) {
                                        ownerList.remove(cUser);
                                        successes++;
                                    } else failures++;
                                }
                                break;
                            case SET:
                                ownerList.clear();
                                for (User user : users) {
                                    ownerList.add(user);
                                    successes++;
                                }
                        }
                        source.sendMessage(Texts.of(TextColors.GREEN, "Modified list with " + successes + " successes and " + failures + " failures."));
                        return true;
                    } else {
                        source.sendMessage(Texts.of(TextColors.RED, "Must specify one or more users!"));
                        return false;
                    }
                } else {
                    source.sendMessage(Texts.of(TextColors.RED, "Must specify an operation!"));
                    return false;
                }

            } else if (isAlias(passiveAliases, args[0])) {

            } else {
                source.sendMessage(Texts.of(TextColors.RED, "Not a valid SimpleFlagset command!"));
                return false;
            }
        } else {
            source.sendMessage(Texts.of(TextColors.RED, "Must specify a command!"));
            return false;
        }
        return false;
    }

    @Override
    public Text getDetails(String arguments) {
        return super.getDetails(arguments);
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
    }

    @Override
    public String getShortTypeName() {
        return "Pass";
    }

    @Override
    public String getLongTypeName() {
        return "Passive";
    }

    @Override
    public String getUniqueTypeString() {
        return "passive";
    }

}
