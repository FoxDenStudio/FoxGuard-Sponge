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
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParse;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;

public class CommandDetail implements CommandCallable {

    private static final Function<Map<String, String>, Function<String, Consumer<String>>> MAPPER = map -> key -> value -> {
        if (isAlias(WORLD_ALIASES, key) && !map.containsKey("world")) {
            map.put("world", value);
        }
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Text.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        AdvCmdParse.ParseResult parse = AdvCmdParse.builder().arguments(arguments).limit(2).flagMapper(MAPPER).parse2();
        if (parse.args.length == 0) {
            source.sendMessage(Text.builder()
                    .append(Text.of(TextColors.GREEN, "Usage: "))
                    .append(getUsage(source))
                    .build());
            return CommandResult.empty();
        } else if (isAlias(REGIONS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));
            String worldName = parse.flagmap.get("world");
            World world = null;
            if (source instanceof Player) world = ((Player) source).getWorld();
            if (!worldName.isEmpty()) {
                Optional<World> optWorld = Sponge.getGame().getServer().getWorld(worldName);
                if (optWorld.isPresent()) {
                    world = optWorld.get();
                }
            }
            if (world == null) throw new CommandException(Text.of("Must specify a world!"));
            IRegion region = FGManager.getInstance().getRegion(world, parse.args[1]);
            if (region == null)
                throw new CommandException(Text.of("No Region with name \"" + parse.args[1] + "\"!"));
            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "-----------------------------------------------------\n"));
            builder.append(Text.of(TextColors.GREEN, "---General---\n"));
            builder.append(Text.of(TextColors.GOLD, "Name: "), Text.of(TextColors.RESET, region.getName() + "\n"));
            builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, region.getLongTypeName() + "\n"));
            builder.append(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Enabled: "))
                    .append(Text.of(TextColors.RESET, (region.isEnabled() ? "True" : "False") + "\n"))
                    .onClick(TextActions.runCommand("/foxguard " + (region.isEnabled() ? "disable" : "enable") +
                            " region --w:" + region.getWorld().getName() + " " + region.getName()))
                    .onHover(TextActions.showText(Text.of("Click to " + (region.isEnabled() ? "Disable" : "Enable"))))
                    .build());
            builder.append(Text.of(TextColors.GOLD, "World: "), Text.of(TextColors.RESET, region.getWorld().getName() + "\n"));
            builder.append(Text.of(TextColors.GREEN, "---Details---\n"));
            builder.append(region.getDetails(parse.args.length < 3 ? "" : parse.args[2]));
            builder.append(Text.of(TextColors.GREEN, "\n---Linked Handlers---"));
            if (region.getHandlers().size() == 0)
                builder.append(Text.of(TextStyles.ITALIC, "\nNo linked Handlers!"));
            region.getHandlers().stream().forEach(handler -> builder.append(Text.of(FGHelper.getColorForHandler(handler),
                    TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                    TextActions.showText(Text.of("View Details")),
                    "\n" + handler.getShortTypeName() + " : " + handler.getName()
            )));
            source.sendMessage(builder.build());
            return CommandResult.empty();
        } else if (isAlias(HANDLERS_ALIASES, parse.args[0])) {
            if (parse.args.length < 2) throw new CommandException(Text.of("Must specify a name!"));

            IHandler handler = FGManager.getInstance().gethandler(parse.args[1]);
            if (handler == null)
                throw new CommandException(Text.of("No Handler with name \"" + parse.args[1] + "\"!"));
            Text.Builder builder = Text.builder();
            builder.append(Text.of(TextColors.GOLD, "-----------------------------------------------------\n"));
            builder.append(Text.of(TextColors.GREEN, "---General---\n"));
            builder.append(Text.of(TextColors.GOLD, "Name: "), Text.of(TextColors.RESET, handler.getName() + "\n"));
            builder.append(Text.of(TextColors.GOLD, "Type: "), Text.of(TextColors.RESET, handler.getLongTypeName() + "\n"));
            builder.append(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Enabled: "))
                    .append(Text.of(TextColors.RESET, (handler.isEnabled() ? "True" : "False") + "\n"))
                    .onClick(TextActions.runCommand("/foxguard " + (handler.isEnabled() ? "disable" : "enable") + " handler " + handler.getName()))
                    .onHover(TextActions.showText(Text.of("Click to " + (handler.isEnabled() ? "Disable" : "Enable"))))
                    .build());
            builder.append(Text.builder()
                    .append(Text.of(TextColors.GOLD, "Priority: "))
                    .append(Text.of(TextColors.RESET, handler.getPriority() + "\n"))
                    .onClick(TextActions.suggestCommand("/foxguard handlers priority " + handler.getName() + " "))
                    .onHover(TextActions.showText(Text.of("Click to Change Priority")))
                    .build());
            builder.append(Text.of(TextColors.GREEN, "---Details---\n"));
            builder.append(handler.getDetails(parse.args.length < 3 ? "" : parse.args[2]));
            source.sendMessage(builder.build());
            return CommandResult.empty();
        } else {
            throw new ArgumentParseException(Text.of("Not a valid category!"), parse.args[0], 0);
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }

    @Override
    public boolean testPermission(CommandSource source) {

        return source.hasPermission("foxguard.command.info.objects.detail");
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
        return Text.of("detail <region [--w:<worldname>] | handler> <name> [parse.args...]");
    }
}
