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
import com.google.common.collect.Lists;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flags;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IHandlerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.FormattingCodeTextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.transform.SimpleTextTemplateApplier;
import org.spongepowered.api.text.transform.TextTemplateApplier;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Fox on 11/5/2016.
 */
public class WelcomeHandler extends HandlerBase {
    private static final Map<String, Object> matches;
    private static final Pattern pattern;

    static {
        pattern = Pattern.compile("&[0-9a-fk-or]|(?:\\.|&[^0-9a-fk-or]|[^&%])+");
        matches = new HashMap<>();
        matches.put("&0", TextColors.BLACK);
        matches.put("&1", TextColors.DARK_BLUE);
        matches.put("&2", TextColors.DARK_GREEN);
        matches.put("&3", TextColors.DARK_AQUA);
        matches.put("&4", TextColors.DARK_RED);
        matches.put("&5", TextColors.DARK_PURPLE);
        matches.put("&6", TextColors.GOLD);
        matches.put("&7", TextColors.GRAY);
        matches.put("&8", TextColors.DARK_GRAY);
        matches.put("&9", TextColors.BLUE);
        matches.put("&a", TextColors.GREEN);
        matches.put("&b", TextColors.DARK_BLUE);
        matches.put("&c", TextColors.DARK_BLUE);
        matches.put("&d", TextColors.DARK_BLUE);
        matches.put("&e", TextColors.DARK_BLUE);
        matches.put("&f", TextColors.DARK_BLUE);
        matches.put("&k", TextStyles.OBFUSCATED);
        matches.put("&l", TextStyles.BOLD);
        matches.put("&m", TextStyles.STRIKETHROUGH);
        matches.put("&n", TextStyles.UNDERLINE);
        matches.put("&o", TextStyles.ITALIC);
        matches.put("&1", TextColors.RESET);
        matches.put("player",  TextTemplate.arg("player").build());
    }

    TextTemplate enterTemplate = TextTemplate.EMPTY;
    TextTemplate exitTemplate = TextTemplate.EMPTY;

    public WelcomeHandler(String name, int priority) {
        this(name, true, priority);
    }

    public WelcomeHandler(String name, boolean isEnabled, int priority) {
        super(name, priority, isEnabled);
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(2).parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify a command!"));

        if(parse.args.length < 2) return ProcessResult.success();

        if(parse.args[0].equals("enter")) enterTemplate = fromString(parse.args[1]);
        else if(parse.args[0].equals("exit")) exitTemplate = fromString(parse.args[1]);
        else ProcessResult.of(false, "Invalid Command.");

        return ProcessResult.success();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        return Lists.newArrayList("enter", "exit");
    }

    @Override
    public EventResult handle(@Nullable User user, FlagBitSet flags, ExtraContext extra) {
        Optional<Player> player = Optional.ofNullable((Player) user);

        if(player.isPresent()) {
            if (flags.get(Flags.ENTER)) player.get().sendMessage(enterTemplate.toText());
            else if (flags.get(Flags.EXIT)) player.get().sendMessage(exitTemplate.toText());
        }

        return EventResult.pass();
    }

    @Override
    public String getShortTypeName() {
        return "Wel";
    }

    @Override
    public String getLongTypeName() {
        return "Welcome";
    }

    @Override
    public String getUniqueTypeString() {
        return "welcome";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Enter:\n"));
        builder.append(enterTemplate.toText());
        builder.append(Text.of(TextColors.GOLD, "Exit:\n"));
        builder.append(exitTemplate.toText());
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
        return ImmutableList.of();
    }

    @Override
    public void save(Path directory) {
        Path file = directory.resolve("messages.cfg");
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(file).build();
        CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(file, loader);
        root.getNode("enter").setValue(enterTemplate);
        root.getNode("exit").setValue(exitTemplate);
    }

    private static TextTemplate fromString(String string) {
        TextTemplate template = TextTemplate.of();

        for(String object : pattern.split(string)) {
            template.concat(TextTemplate.of(matches.getOrDefault(object, object)));
        }

        return template;
    }

    public static class Factory implements IHandlerFactory {

        @Override
        public IHandler create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            return null;
        }

        @Override
        public IHandler create(Path directory, String name, int priority, boolean isEnabled) {
            return null;
        }

        @Override
        public String[] getAliases() {
            return new String[0];
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getPrimaryAlias() {
            return null;
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return null;
        }
    }
}
