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

package net.foxdenstudio.sponge.foxguard.plugin.state;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.state.ListStateFieldBase;
import net.foxdenstudio.sponge.foxcore.plugin.state.SourceState;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ControllersStateField extends ListStateFieldBase<IController> {

    public static final String ID = "controller";

    public ControllersStateField(SourceState sourceState) {
        super("Controllers", sourceState);
    }

    @Override
    public Text currentState(CommandSource source) {
        Text.Builder builder = Text.builder();
        int index = 1;
        for (Iterator<IController> it = this.list.iterator(); it.hasNext(); ) {
            IController controller = it.next();
            if (source instanceof Player) {
                builder.append(Text.of(TextColors.RED,
                        TextActions.runCommand("/foxguard s c remove " + controller.getName()),
                        TextActions.showText(Text.of("Remove from Handler State Buffer")),
                        "  [-] "));
            }
            builder.append(Text.of(FGUtil.getColorForObject(controller), (index++) + ": " + controller.getShortTypeName() + " : " + controller.getName()));
            if (it.hasNext()) builder.append(Text.NEW_LINE);
        }
        return builder.build();
    }

    @Override
    public Text detailedState(CommandSource source, String args) {
        return currentState(source);
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).limit(1).parseLastFlags(false).parse();
        String newArgs = parse.args.length > 1 ? parse.args[1] : "";
        if (parse.args.length == 0 || parse.args[0].equalsIgnoreCase("add")) {
            return add(source, newArgs);
        } else if (parse.args[0].equalsIgnoreCase("remove")) {
            return remove(source, newArgs);
        }

        return ProcessResult.of(false, Text.of("Not a valid controller state command!"));
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
                return ImmutableList.of("add", "remove").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (parse.args[0].equals("add")) {
                    return FGManager.getInstance().getControllers().stream()
                            .filter(handler -> !this.list.contains(handler))
                            .map(IFGObject::getName)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (parse.args[0].equals("remove")) {
                    return this.list.stream()
                            .map(IFGObject::getName)
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
    public Optional<Text> getScoreboardTitle() {
        return Optional.of(Text.of(TextColors.GREEN, this.name,
                TextColors.YELLOW, " (", this.list.size(), ")"));
    }

    @Override
    public List<Text> getScoreboardText() {
        int[] index = {1};
        return this.list.stream()
                .map(controller -> Text.of("  " + index[0]++ + ": " + controller.getShortTypeName() + " : " + controller.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean showScoreboard() {
        return !list.isEmpty();
    }

    @Override
    public boolean prioritizeLast() {
        return false;
    }

    public ProcessResult add(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name!"));
        IController controller = FGManager.getInstance().getController(parse.args[0]);
        if (controller == null)
            throw new ArgumentParseException(Text.of("No controllers with this name!"), parse.args[0], 1);
        if (this.list.contains(controller))
            throw new ArgumentParseException(Text.of("Controller is already in your state buffer!"), parse.args[0], 1);
        this.list.add(controller);
        return ProcessResult.of(true, Text.of("Successfully added controller to your state buffer!"));
    }

    public ProcessResult remove(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder().arguments(arguments).parse();

        if (parse.args.length < 1) throw new CommandException(Text.of("Must specify a name or a number!"));
        if (parse.args.length == 1) {
            IController controller;
            try {
                int index = Integer.parseInt(parse.args[0]);
                controller = this.list.get(index - 1);
            } catch (NumberFormatException e) {
                controller = FGManager.getInstance().getController(parse.args[0]);
            } catch (IndexOutOfBoundsException e) {
                throw new ArgumentParseException(Text.of("Index out of bounds! (1 - " + this.list.size()), parse.args[0], 1);
            }
            if (controller == null)
                throw new ArgumentParseException(Text.of("No controllers with this name!"), parse.args[0], 1);
            if (!this.list.contains(controller))
                throw new ArgumentParseException(Text.of("Controller is not in your state buffer!"), parse.args[0], 1);
            this.list.remove(controller);
            return ProcessResult.of(true, Text.of("Successfully removed controller from your state buffer!"));
        } else {
            int successes = 0, failures = 0;
            for (String arg : parse.args) {
                IController controller;
                try {
                    int index = Integer.parseInt(arg);
                    controller = this.list.get(index - 1);
                } catch (NumberFormatException e) {
                    controller = FGManager.getInstance().getController(arg);
                } catch (IndexOutOfBoundsException e) {
                    failures++;
                    continue;
                }
                if (controller == null) {
                    failures++;
                    continue;
                }
                if (!this.list.contains(controller)) {
                    failures++;
                    continue;
                }
                this.list.remove(controller);
                successes++;
            }
            if (successes > 0) {
                sourceState.updateScoreboard();
                return ProcessResult.of(true, Text.of(TextColors.GREEN, "Successfully removed controllers from your state buffer with "
                        + successes + " successes" + (failures > 0 ? " and " + failures + " failures!" : "!")));
            } else {
                return ProcessResult.of(false, Text.of(failures + " failures while trying to remove controllers from your state buffer. " +
                        "Check that their names or indices are valid."));
            }
        }
    }

    public List<String> removeSuggestions(CommandSource source, String arguments) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.ARGUMENT)) {
            if (parse.current.index == 0)
                return this.list.stream()
                        .map(IFGObject::getName)
                        .filter(new StartsWithPredicate(parse.current.token))
                        .collect(GuavaCollectors.toImmutableList());
        } else if (parse.current.type.equals(AdvCmdParser.CurrentElement.ElementType.COMPLETE))
            return ImmutableList.of(parse.current.prefix + " ");
        return ImmutableList.of();
    }
}
