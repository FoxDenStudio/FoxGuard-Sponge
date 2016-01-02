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

package net.foxdenstudio.sponge.foxguard.plugin.object.factory;

import net.foxdenstudio.sponge.foxcore.common.FCHelper;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.SourceState;
import net.foxdenstudio.sponge.foxcore.plugin.util.Aliases;
import net.foxdenstudio.sponge.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.PassiveHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.PermissionHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.SimpleHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Flag;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.Tristate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FGHandlerFactory implements IHandlerFactory {

    private final String[] simpleAliases = {"simple", "simp"};
    private final String[] passiveAliases = {"passive", "pass"};
    private final String[] permissionAliases = {"permission", "permissions", "perm", "perms"};
    private final String[] types = {"simple", "passive", "permission"};

    @Override
    public String[] getAliases() {
        return FCHelper.concatAll(simpleAliases, passiveAliases, permissionAliases);
    }

    @Override
    public String[] getTypes() {
        return types;
    }

    @Override
    public IHandler createHandler(String name, String type, int priority, String arguments, SourceState state, CommandSource source) {
        if (Aliases.isAlias(simpleAliases, type)) {
            SimpleHandler handler = new SimpleHandler(name, priority);
            if (source instanceof Player) handler.addOwner((Player) source);
            return handler;
        } else if (Aliases.isAlias(passiveAliases, type)) {
            PassiveHandler handler = new PassiveHandler(name, priority);
            if (source instanceof Player) handler.addOwner((Player) source);
            return handler;
        } else if (Aliases.isAlias(permissionAliases, type)) {
            return new PermissionHandler(name, priority);
        } else return null;
    }

    @Override
    public IHandler createHandler(DataSource source, String name, String type, int priority, boolean isEnabled) throws SQLException {
        if (type.equalsIgnoreCase("simple")) {
            List<User> ownerList = new LinkedList<>();
            List<User> memberList = new LinkedList<>();
            SimpleHandler.PassiveOptions po = SimpleHandler.PassiveOptions.DEFAULT;
            CallbackHashMap<Flag, Tristate> ownerFlagMap = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
            CallbackHashMap<Flag, Tristate> memberFlagMap = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
            CallbackHashMap<Flag, Tristate> defaultFlagMap = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent() && !FCHelper.isUserOnList(ownerList, user.get()))
                                ownerList.add(user.get());
                        }
                    }
                    try (ResultSet memberSet = statement.executeQuery("SELECT * FROM MEMBERS")) {
                        while (memberSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) memberSet.getObject("USERUUID"));
                            if (user.isPresent() && !FCHelper.isUserOnList(memberList, user.get()))
                                memberList.add(user.get());
                        }
                    }
                    try (ResultSet mapSet = statement.executeQuery("SELECT * FROM MAP")) {
                        while (mapSet.next()) {
                            String key = mapSet.getString("KEY");
                            switch (key) {
                                case "passive":
                                    try {
                                        po = SimpleHandler.PassiveOptions.valueOf(mapSet.getString("VALUE"));
                                    } catch (IllegalArgumentException ignored) {
                                        po = SimpleHandler.PassiveOptions.PASSTHROUGH;
                                    }
                                    break;
                            }
                        }
                    }
                    try (ResultSet passiveMapEntrySet = statement.executeQuery("SELECT * FROM OWNERFLAGMAP")) {
                        while (passiveMapEntrySet.next()) {
                            try {
                                ownerFlagMap.put(Flag.valueOf(passiveMapEntrySet.getString("KEY")),
                                        Tristate.valueOf(passiveMapEntrySet.getString("VALUE")));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    try (ResultSet passiveMapEntrySet = statement.executeQuery("SELECT * FROM MEMBERFLAGMAP")) {
                        while (passiveMapEntrySet.next()) {
                            try {
                                memberFlagMap.put(Flag.valueOf(passiveMapEntrySet.getString("KEY")),
                                        Tristate.valueOf(passiveMapEntrySet.getString("VALUE")));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    try (ResultSet passiveMapEntrySet = statement.executeQuery("SELECT * FROM DEFAULTFLAGMAP")) {
                        while (passiveMapEntrySet.next()) {
                            try {
                                defaultFlagMap.put(Flag.valueOf(passiveMapEntrySet.getString("KEY")),
                                        Tristate.valueOf(passiveMapEntrySet.getString("VALUE")));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            }
            SimpleHandler handler = new SimpleHandler(name, priority, ownerFlagMap, memberFlagMap, defaultFlagMap);
            handler.setOwners(ownerList);
            handler.setMembers(memberList);
            handler.setPassiveOption(po);
            handler.setIsEnabled(isEnabled);
            return handler;
        } else if (type.equalsIgnoreCase("passive")) {
            List<User> ownerList = new LinkedList<>();
            CallbackHashMap<Flag, Tristate> flagMap = new CallbackHashMap<>((key, map) -> Tristate.UNDEFINED);
            try (Connection conn = source.getConnection()) {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet ownerSet = statement.executeQuery("SELECT * FROM OWNERS")) {
                        while (ownerSet.next()) {
                            Optional<User> user = FoxGuardMain.instance().getUserStorage().get((UUID) ownerSet.getObject("USERUUID"));
                            if (user.isPresent() && !FCHelper.isUserOnList(ownerList, user.get()))
                                ownerList.add(user.get());
                        }
                    }
                    try (ResultSet passiveMapEntrySet = statement.executeQuery("SELECT * FROM FLAGMAP")) {
                        while (passiveMapEntrySet.next()) {
                            try {
                                flagMap.put(Flag.valueOf(passiveMapEntrySet.getString("KEY")),
                                        Tristate.valueOf(passiveMapEntrySet.getString("VALUE")));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            }
            PassiveHandler handler = new PassiveHandler(name, priority, flagMap);
            handler.setOwners(ownerList);
            handler.setIsEnabled(isEnabled);
            return handler;
        } else if (type.equalsIgnoreCase("permission")) {
            PermissionHandler handler = new PermissionHandler(name, priority);
            handler.setIsEnabled(isEnabled);
            return handler;
        } else return null;
    }
}
