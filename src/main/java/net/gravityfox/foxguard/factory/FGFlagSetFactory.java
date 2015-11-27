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

package net.gravityfox.foxguard.factory;

import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.flagsets.SimpleFlagSet;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.command.CommandSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public class FGFlagSetFactory implements IFlagSetFactory {

    String[] simpleAliases = {"simple"};
    String[] types = {"simple"};

    @Override
    public IFlagSet createFlagSet(String name, String type, int priority, String arguments, InternalCommandState state, CommandSource source) {
        if (type.equalsIgnoreCase("simple")) {
            SimpleFlagSet flagSet = new SimpleFlagSet(name, priority);
            if (source instanceof Player) flagSet.addOwner((Player) source);
            return flagSet;
        } else return null;
    }

    @Override
    public IFlagSet createFlagSet(DataSource source, String name, String type, int priority) throws SQLException {
        if (type.equalsIgnoreCase("simple")) {
            List<User> ownerList = new LinkedList<>();
            List<User> memberList = new LinkedList<>();
            SimpleFlagSet.PassiveOptions po = SimpleFlagSet.PassiveOptions.DEFAULT;
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent() && !FGHelper.isUserOnList(ownerList, user.get()))
                                ownerList.add(user.get());
                        }
                    }
                    try (ResultSet memberSet = statement.executeQuery("SELECT * FROM MEMBERS")) {
                        while (memberSet.next()) {
                            Optional<User> user = FoxGuardMain.getInstance().getUserStorage().get((UUID) memberSet.getObject("USERUUID"));
                            if (user.isPresent() && !FGHelper.isUserOnList(memberList, user.get()))
                                memberList.add(user.get());
                        }
                    }
                    try (ResultSet mapSet = statement.executeQuery("SELECT * FROM MAP")) {
                        while (mapSet.next()) {
                            String key = mapSet.getString("KEYCOL");
                            switch (key) {
                                case "passive":
                                    po = SimpleFlagSet.PassiveOptions.from(mapSet.getString("VALUECOL"));
                                    break;
                            }
                        }
                    }
                }
            }
            SimpleFlagSet flagSet = new SimpleFlagSet(name, priority);
            flagSet.setOwners(ownerList);
            flagSet.setMembers(memberList);
            flagSet.setPassiveOption(po);
            return flagSet;
        } else return null;
    }

    @Override
    public String[] getAliases() {
        return FGHelper.concatAll(simpleAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }
}
