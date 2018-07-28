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
import com.google.common.collect.ImmutableMap;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagSet;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by Fox on 11/5/2016.
 */
public class WelcomeHandler extends HandlerBase {

    private static final Pattern PATTERN = Pattern.compile("&[0-9a-fk-or]|%|(?:\\.|&(?![0-9a-fk-or%])|[^&%])+");

    private boolean enter;
    private boolean exit;
    private String enterString = "";
    private String exitString = "";
    private TextTemplate enterTemplate = TextTemplate.EMPTY;
    private TextTemplate exitTemplate = TextTemplate.EMPTY;


    public WelcomeHandler(HandlerData data) {
        this(data, "", "");
    }

    public WelcomeHandler(HandlerData data, String enterString, String exitString) {
        super(data);
        this.setEnter(enterString);
        this.setExit(exitString);
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(1)
                .leaveFinalAsIs(true)
                .parse();

        if (parse.args.length < 1) return ProcessResult.of(false, Text.of("Must specify a command!"));

        if (parse.args.length < 2) return ProcessResult.failure();

        if (parse.args[0].equalsIgnoreCase("enter")) this.setEnter(parse.args[1]);
        else if (parse.args[0].equalsIgnoreCase("exit")) this.setExit(parse.args[1]);
        else ProcessResult.of(false, "Invalid Command!");

        return ProcessResult.success();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .limit(1)
                .leaveFinalAsIs(true)
                .autoCloseQuotes(true)
                .excludeCurrent(true)
                .parse();
        if (parse.current.type == AdvCmdParser.CurrentElement.ElementType.ARGUMENT) {
            if (parse.current.index == 0) {
                return Stream.of("enter", "exit")
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            }
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public EventResult handle(@Nullable User user, FlagSet flags, ExtraContext extra) {
        if (flags.get(Flags.MOVE)) {
            Player player = (Player) user;
            Map<String, String> input = ImmutableMap.of("player", player.getName());
            if (flags.get(Flags.ENTER)) {
                player.sendMessage(enterTemplate.apply(input).build());
                //player.sendMessage(enterTemplate.toText());
            } else if (flags.get(Flags.EXIT)) {
                player.sendMessage(exitTemplate.apply(input).build());
            }
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
        final Text quote = Text.of(TextColors.AQUA, "\"");
        Text.Builder builder = Text.builder();
        builder.append(Text.of(TextColors.GREEN, "Enter:\n  "));
        builder.append(quote).append(enterTemplate.toText()).append(quote);
        builder.append(Text.of(TextColors.GOLD, "\nExit:\n  "));
        builder.append(quote).append(exitTemplate.toText()).append(quote);
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
        root.getNode("enter").setValue(enterString);
        root.getNode("exit").setValue(exitString);
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setEnter(String enterString) {
        this.enter = !enterString.isEmpty();
        this.enterString = enterString;
        this.enterTemplate = fromString(enterString);
    }

    public void setExit(String exitString) {
        this.exit = !exitString.isEmpty();
        this.exitString = exitString;
        this.exitTemplate = fromString(exitString);
    }

    private static TextTemplate fromString(String string) {
        List<Object> elements = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(string);

        TextColor curColor = TextColors.RESET;
        TextStyle curStyle = TextStyles.RESET;
        while (matcher.find()) {
            String str = matcher.group();

            if (str.equals("%")) {
                elements.add(TextTemplate.arg("player")
                        .color(curColor)
                        .style(curStyle)
                        .build());
            } else if (str.matches("&[0-9a-fk-or]")) {
                str = str.substring(1);
                if (str.matches("[0-9a-f]")) {
                    Optional<TextColor> colorOpt = FCPUtil.textColorFromHex(str);
                    if (colorOpt.isPresent()) curColor = colorOpt.get();
                } else {
                    Optional<TextStyle> styleOpt = FCPUtil.textStyleFromCode(str);
                    if (styleOpt.isPresent()) curStyle = styleOpt.get();
                }
            } else {
                elements.add(Text.of(curColor, curStyle, str));
            }
        }

        return TextTemplate.of(elements.toArray());
    }

    public static class Factory implements IHandlerFactory {

        private static final String[] ALIASES = {"welcome"};

        @Override
        public IHandler create(String name, String arguments, CommandSource source) throws CommandException {
            return new WelcomeHandler(new HandlerData().setName(name));
        }

        @Override
        public IHandler create(Path directory, HandlerData data) {
            Path file = directory.resolve("messages.cfg");
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(file).build();
            CommentedConfigurationNode root = FCPUtil.getHOCONConfiguration(file, loader);
            String enterString = root.getNode("enter").getString("");
            String exitString = root.getNode("enter").getString("");
            return new WelcomeHandler(data, enterString, exitString);
        }

        @Override
        public String[] getAliases() {
            return ALIASES;
        }

        @Override
        public String getType() {
            return "welcome";
        }

        @Override
        public String getPrimaryAlias() {
            return "welcome";
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return ImmutableList.of();
        }
    }
}
