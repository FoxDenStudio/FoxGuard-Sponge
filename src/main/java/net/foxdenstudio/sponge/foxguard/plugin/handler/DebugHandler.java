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
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Fox on 7/4/2016.
 */
public class DebugHandler extends HandlerBase {

    public final Set<UUID> members;
    public boolean console;
    private TextColor color = TextColors.WHITE;

    public DebugHandler(String name, int priority) {
        this(name, true, priority, new HashSet<>(), false, TextColors.WHITE);
    }

    public DebugHandler(String name, boolean isEnabled, int priority, Set<UUID> members, boolean console, TextColor color) {
        super(name, isEnabled, priority);
        this.members = members;
        this.console = console;
        this.color = color;
    }

    @Override
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        UserStorageService storageService = FoxGuardMain.instance().getUserStorage();

        Text.Builder builder = Text.builder();
        builder.append(Text.of(color, "[FG " + this.name + "] "));
        if (user == null) {
            builder.append(Text.of(TextColors.WHITE, "None"));
        } else {
            builder.append(Text.of(user.isOnline() ? TextColors.GREEN : TextColors.YELLOW, user.getName()));
        }
        builder.append(Text.of(TextColors.RESET, " :"));
        flags.toFlagSet().stream().forEachOrdered(flag -> builder.append(Text.of(" " + flag.name)));
        Text message = builder.build();
        for (UUID uuid : this.members) {
            Optional<User> listenerOpt = storageService.get(uuid);
            if (listenerOpt.isPresent()) {
                Optional<Player> playerOpt = listenerOpt.get().getPlayer();
                if (playerOpt.isPresent()) {
                    playerOpt.get().sendMessage(message);
                }
            }
        }
        if (console) Sponge.getServer().getConsole().sendMessage(message);
        return EventResult.pass();
    }

    @Override
    public String getShortTypeName() {
        return "Debug";
    }

    @Override
    public String getLongTypeName() {
        return "Debug";
    }

    @Override
    public String getUniqueTypeString() {
        return "debug";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        UserStorageService userStorageService = FoxGuardMain.instance().getUserStorage();
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.AQUA, "Color: ")).append(Text.of(this.color, this.color.getName(), "\n"));
        builder.append(Text.of(TextColors.AQUA, "Console: ")).append(Text.of(console ? TextColors.GREEN: TextColors.RED, Boolean.toString(console), "\n"));
        builder.append(Text.of(TextColors.GREEN, "Members: "));
        for(UUID uuid : this.members){
            Optional<User> userOptional = userStorageService.get(uuid);
            if (userOptional.isPresent()) {
                TextColor color = TextColors.WHITE;
                User u = userOptional.get();
                if (source instanceof Player && ((Player) source).getUniqueId().equals(uuid))
                    color = TextColors.YELLOW;
                builder.append(Text.of(color, u.getName())).append(Text.of(" "));
            } else {
                builder.append(Text.of(TextColors.RESET, uuid.toString())).append(Text.of(" "));
            }
        }
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        UserStorageService userStorageService = FoxGuardMain.instance().getUserStorage();
        Path configFile = directory.resolve("config.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(configFile).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(configFile, loader);
        List<Map<String, Object>> members = new ArrayList<>();
        for (UUID uuid : this.members) {
            Optional<User> userOptional = userStorageService.get(uuid);
            Map<String, Object> map = new HashMap<>();
            if (userOptional.isPresent()) map.put("username", userOptional.get().getName());
            map.put("uuid", uuid.toString());
            members.add(map);
        }
        root.getNode("members").setValue(members);
        root.getNode("console").setValue(this.console);
        root.getNode("color").setValue(this.color.getName());
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    public void setColor(TextColor color) {
        if (color != null) this.color = color;
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] ALIASES = {"debug", "debugger", "info"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            DebugHandler handler = new DebugHandler(name, priority);
            if (source instanceof Player) handler.members.add(((Player) source).getUniqueId());
            else if(source instanceof ConsoleSource) handler.console = true;
            return handler;
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            UserStorageService userStorageService = FoxGuardMain.instance().getUserStorage();
            Path configFile = directory.resolve("config.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(configFile).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(configFile, loader);
            List<Optional<UUID>> optionalMemberUUIDsList = root.getNode("members").getList(o -> {
                if (o instanceof HashMap) {
                    HashMap map = (HashMap) o;
                    return Optional.of(UUID.fromString((String) map.get("uuid")));
                } else return Optional.empty();
            });
            Set<UUID> members = optionalMemberUUIDsList.stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            boolean console = root.getNode("console").getBoolean(false);
            TextColor color = Sponge.getRegistry().getType(TextColor.class, root.getNode("color").getString("white")).orElse(TextColors.WHITE);
            return new DebugHandler(name, isEnabled, priority, members, console, color);
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "debug";
        }

        @Override
        public String getPrimaryAlias() {
            return getType();
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
            return ImmutableList.of();
        }
    }
}
