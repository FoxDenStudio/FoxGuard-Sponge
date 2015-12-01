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

package net.gravityfox.foxguard.commands;

import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.*;

public class CommandDetail implements CommandCallable {

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +", 3);
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (isAlias(REGIONS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                    args = arguments.split(" +", 4);
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                IRegion region = FGManager.getInstance().getRegion(world, args[1 + flag]);
                if (region == null)
                    throw new CommandException(Texts.of("No Region with name \"" + args[1 + flag] + "\"!"));
                TextBuilder builder = Texts.builder();
                builder.append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"));
                builder.append(Texts.of(TextColors.GREEN, "---General---\n"));
                builder.append(Texts.of(TextColors.GOLD, "Name: "), Texts.of(TextColors.RESET, region.getName() + "\n"));
                builder.append(Texts.of(TextColors.GOLD, "Type: "), Texts.of(TextColors.RESET, region.getLongTypeName() + "\n"));
                builder.append(Texts.builder()
                        .append(Texts.of(TextColors.GOLD, "Enabled: "))
                        .append(Texts.of(TextColors.RESET, (region.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (region.isEnabled() ? "disable" : "enable") +
                                " region w:" + region.getWorld().getName() + " " + region.getName()))
                        .onHover(TextActions.showText(Texts.of("Click to " + (region.isEnabled() ? "Disable" : "Enable"))))
                        .build());
                builder.append(Texts.of(TextColors.GOLD, "World: "), Texts.of(TextColors.RESET, region.getWorld().getName() + "\n"));
                builder.append(Texts.of(TextColors.GREEN, "---Details---\n"));
                builder.append(region.getDetails(args.length < 3 + flag ? "" : args[2 + flag]));
                builder.append(Texts.of(TextColors.GREEN, "\n---Linked Handlers---"));
                if (region.getHandlersCopy().size() == 0)
                    builder.append(Texts.of(TextStyles.ITALIC, "\nNo linked Handlers!"));
                region.getHandlersCopy().stream().forEach(handler -> builder.append(Texts.of(FGHelper.getColorForHandler(handler),
                        TextActions.runCommand("/foxguard detail handler " + handler.getName()),
                        TextActions.showText(Texts.of("View Details")),
                        "\n" + handler.getShortTypeName() + " : " + handler.getName()
                )));
                player.sendMessage(builder.build());

            } else if (isAlias(HANDLERS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));

                IHandler handler = FGManager.getInstance().gethandler(args[1]);
                if (handler == null)
                    throw new CommandException(Texts.of("No Handler with name \"" + args[1] + "\"!"));
                TextBuilder builder = Texts.builder();
                builder.append(Texts.of(TextColors.GOLD, "-----------------------------------------------------\n"));
                builder.append(Texts.of(TextColors.GREEN, "---General---\n"));
                builder.append(Texts.of(TextColors.GOLD, "Name: "), Texts.of(TextColors.RESET, handler.getName() + "\n"));
                builder.append(Texts.of(TextColors.GOLD, "Type: "), Texts.of(TextColors.RESET, handler.getLongTypeName() + "\n"));
                builder.append(Texts.builder()
                        .append(Texts.of(TextColors.GOLD, "Enabled: "))
                        .append(Texts.of(TextColors.RESET, (handler.isEnabled() ? "True" : "False") + "\n"))
                        .onClick(TextActions.runCommand("/foxguard " + (handler.isEnabled() ? "disable" : "enable") + " handler " + handler.getName()))
                        .onHover(TextActions.showText(Texts.of("Click to " + (handler.isEnabled() ? "Disable" : "Enable"))))
                        .build());
                builder.append(Texts.builder()
                        .append(Texts.of(TextColors.GOLD, "Priority: "))
                        .append(Texts.of(TextColors.RESET, handler.getPriority() + "\n"))
                        .onClick(TextActions.suggestCommand("/foxguard handlers priority " + handler.getName() + " "))
                        .onHover(TextActions.showText(Texts.of("Click to Change Priority")))
                        .build());
                builder.append(Texts.of(TextColors.GREEN, "---Details---\n"));
                builder.append(handler.getDetails(args.length < 3 ? "" : args[2]));
                player.sendMessage(builder.build());
            } else {
                throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
            }
        } else {

        }

        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return new ArrayList<>();
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
        if (source instanceof Player)
            return Texts.of("detail <region [w:<worldname>] | handler> <name> [args...]");
        else return Texts.of("detail <region <worldname> | handler> <name> [args...]");

    }
}
