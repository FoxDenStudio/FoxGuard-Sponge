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

package net.foxdenstudio.sponge.foxguard.pluginold.handler;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.pluginold.flag.FlagSet;
import net.foxdenstudio.sponge.foxguard.pluginold.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.pluginold.util.ExtraContext;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by Fox on 11/5/2016.
 */
public class WelcomeHandler extends HandlerBase {

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
        return null;
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        return null;
    }

    @Override
    public EventResult handle(@Nullable User user, FlagSet flags, ExtraContext extra) {
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
}
