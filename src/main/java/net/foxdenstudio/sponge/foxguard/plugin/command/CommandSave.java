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

package net.foxdenstudio.sponge.foxguard.plugin.command;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.FCCommandBase;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.FlagMapper;
import net.foxdenstudio.sponge.foxguard.plugin.FGStorageManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.FORCE_ALIASES;
import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.isIn;

public class CommandSave extends FCCommandBase {

    private static final FlagMapper MAPPER = map -> key -> value -> {
        map.put(key, value);
        if (isIn(FORCE_ALIASES, key) && !map.containsKey("force")) {
            map.put("force", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }

        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).flagMapper(MAPPER).parse();

        boolean force = parse.flags.containsKey("force");

        FoxGuardMain.instance().getLogger().info(force ? "Force saving objects" : "Saving objects");
        FGStorageManager.getInstance().saveRegions(force);
        Sponge.getServer().getWorlds().forEach(world -> FGStorageManager.getInstance().saveWorldRegions(world, force));
        FGStorageManager.getInstance().saveHandlers(force);
        source.sendMessage(Text.of(TextColors.GREEN, "Successfully saved!"));
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.save");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("save [-f]");
    }

}
