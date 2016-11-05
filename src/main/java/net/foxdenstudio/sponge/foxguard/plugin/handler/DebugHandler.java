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
import net.foxdenstudio.sponge.foxcore.common.util.FCCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Operation;
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
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

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
        super(name, priority, isEnabled);
        this.members = members;
        this.console = console;
        this.color = color;
    }

    @Override
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        /*Optional<Event> eventOptional = extra.first(Event.class);
        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            if (event instanceof ExplosionEvent) {
                DebugHelper.printEvent(event);
            }
        }*/

        UserStorageService storageService = FoxGuardMain.instance().getUserStorage();

        for (UUID uuid : this.members) {
            Optional<User> listenerOpt = storageService.get(uuid);
            if (listenerOpt.isPresent()) {
                Optional<Player> playerOpt = listenerOpt.get().getPlayer();
                if (playerOpt.isPresent()) {
                    Player player = playerOpt.get();
                    Text.Builder builder = Text.builder();
                    builder.append(Text.of(color, "[FG " + this.name + "] "));
                    if (user == null) {
                        builder.append(Text.of(TextColors.WHITE, "None"));
                    } else {
                        builder.append(Text.of(user.getUniqueId().equals(player.getUniqueId()) ? TextColors.AQUA : (user.isOnline() ? TextColors.GREEN : TextColors.YELLOW), user.getName()));
                    }
                    builder.append(Text.of(TextColors.RESET, " :"));
                    flags.toFlagSet().stream().forEachOrdered(flag -> builder.append(Text.of(" " + flag.name)));
                    player.sendMessage(builder.build());
                }
            }
        }
        Text.Builder builder = Text.builder();
        builder.append(Text.of(color, "[FG " + this.name + "] "));
        if (user == null) {
            builder.append(Text.of(TextColors.WHITE, "None"));
        } else {
            builder.append(Text.of(user.isOnline() ? TextColors.GREEN : TextColors.YELLOW, user.getName()));
        }
        builder.append(Text.of(TextColors.RESET, " :"));
        flags.toFlagSet().stream().forEachOrdered(flag -> builder.append(Text.of(" " + flag.name)));
        if (console) Sponge.getServer().getConsole().sendMessage(builder.build());
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
        builder.append(Text.of(TextColors.AQUA, "Console: ")).append(Text.of(console ? TextColors.GREEN : TextColors.RED, Boolean.toString(console), "\n"));
        builder.append(Text.of(TextColors.GREEN, "Members: "));
        for (UUID uuid : this.members) {
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
    public List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .parse();
        if (parse.args.length > 0) {
            if (parse.args[0].equalsIgnoreCase("members")) {
                if (parse.args.length < 2) return ProcessResult.of(false, Text.of("Must specify an operation!"));
                Operation op;
                switch (parse.args[1].toLowerCase()) {
                    case "add":
                        op = Operation.ADD;
                        break;
                    case "remove":
                        op = Operation.REMOVE;
                        break;
                    case "set":
                        op = Operation.SET;
                        break;
                    default:
                        return ProcessResult.of(false, Text.of("Not a valid operation!"));
                }
                if (parse.args.length < 3) {
                    if (op != Operation.SET) {
                        return ProcessResult.of(false, Text.of("Must specify a user or a group to add!"));
                    } else {
                        this.members.clear();
                        return ProcessResult.of(true, Text.of("Successfully cleared members!"));
                    }
                }
                Set<UUID> users = new HashSet<>();
                int successes = 0, failures = 0;
                String[] newArgs = Arrays.copyOfRange(parse.args, 2, parse.args.length);
                for (String s : newArgs) {
                    try {
                        UUID uuid = UUID.fromString(s);
                        users.add(uuid);
                    } catch (IllegalArgumentException e) {
                        Optional<User> userOptional = FoxGuardMain.instance().getUserStorage().get(s);
                        if (userOptional.isPresent()) {
                            users.add(userOptional.get().getUniqueId());
                        } else if (newArgs.length == 1) {
                            return ProcessResult.of(false, Text.of("No user exists with this name!"));
                        } else {
                            failures++;
                        }
                    }
                }
                switch (op) {
                    case ADD:
                        for (UUID u : users) {
                            if (this.members.add(u)) {
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                        break;
                    case REMOVE:
                        for (UUID u : users) {
                            if (this.members.remove(u)) {
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                        break;
                    case SET:
                        this.members.clear();
                        for (UUID u : users) {
                            if (this.members.add(u)) {
                                successes++;
                            } else {
                                failures++;
                            }
                        }
                        break;
                }
                if (failures == 0) {
                    if (successes == 1)
                        return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " user!"));
                    else {
                        return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " " + successes + " users!"));
                    }
                } else {
                    if (successes > 0) {
                        return ProcessResult.of(true, Text.of("Successfully " + op.pastTense + " " + successes + " users with " + failures + " failures!"));
                    } else {
                        return ProcessResult.of(false, Text.of("Failed to " + op.pastTense + " " + failures + " users!"));
                    }
                }
            } else if (parse.args[0].equalsIgnoreCase("console")) {
                if (parse.args.length > 1) {
                    if (isIn(TRUE_ALIASES, parse.args[1])) {
                        this.console = true;
                    } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                        this.console = false;
                    } else {
                        return ProcessResult.of(false, Text.of("Not a valid boolean value!"));
                    }
                    return ProcessResult.of(true, Text.of("Successfully turned " + (this.console ? "on" : "off") + " output!"));
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a boolean value!"));
                }
            } else if (parse.args[0].equalsIgnoreCase("color")) {
                if (parse.args.length > 1) {
                    Optional<TextColor> colorOptional = FCPUtil.textColorFromHex(parse.args[1]);
                    if (!colorOptional.isPresent())
                        colorOptional = FCPUtil.textColorFromName(parse.args[1]);
                    if (colorOptional.isPresent()) {
                        this.color = colorOptional.get();
                        return ProcessResult.of(true, Text.of(TextColors.GREEN, "Successfuly set color to ",
                                this.color, FCPUtil.getColorName(this.color, false),
                                TextColors.GREEN, "!"));
                    } else return ProcessResult.of(false, Text.of("Not a valid color!"));
                } else {
                    return ProcessResult.of(false, Text.of("Must specify a color!"));
                }
            } else {
                return ProcessResult.of(false, Text.of("Not a valid debug handler command!"));
            }
        } else {
            return ProcessResult.of(false, Text.of("Must specify a debug handler command!"));
        }
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0) {
                return ImmutableList.of("members", "console", "color").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (parse.args[0].equalsIgnoreCase("members")) {
                    return ImmutableList.of("add", "remove", "set").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.args[0].equalsIgnoreCase("console")) {
                    return ImmutableList.of("true", "false", "color").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.args[0].equalsIgnoreCase("color")) {
                    return Arrays.stream(FCCUtil.colorNames)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            } else if (parse.current.index >= 2) {
                if (parse.args[0].equalsIgnoreCase("members")) {
                    String[] entries = Arrays.copyOfRange(parse.args, 2, parse.args.length);
                    Operation op;
                    switch (parse.args[1].toLowerCase()) {
                        case "add":
                            op = Operation.ADD;
                            break;
                        case "remove":
                            op = Operation.REMOVE;
                            break;
                        case "set":
                            op = Operation.SET;
                            break;
                        default:
                            op = null;
                    }
                    return Sponge.getServer().getOnlinePlayers().stream()
                            .filter(player -> Operation.userFilter(op, members.contains(player.getUniqueId())))
                            .map(Player::getName)
                            .filter(entry -> !isIn(entries, entry))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                }
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
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

    public void setColor(TextColor color) {
        if (color != null) this.color = color;
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] ALIASES = {"debug", "debugger", "info"};

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            DebugHandler handler = new DebugHandler(name, priority);
            if (source instanceof Player) handler.members.add(((Player) source).getUniqueId());
            else if (source instanceof ConsoleSource) handler.console = true;
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
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return ImmutableList.of();
        }
    }
}
