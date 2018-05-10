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

package net.foxdenstudio.sponge.foxguard.plugin.controller;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.AdvCmdParser;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IControllerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;
import static org.spongepowered.api.text.format.TextColors.*;
import static org.spongepowered.api.util.Tristate.*;

/**
 * Created by Fox on 3/27/2016.
 */
public class LogicController extends ControllerBase {

    public static final String[] OPERATOR_ALIASES = {"o", "op", "operator", "operate", "operation"};
    public static final String[] MODE_ALIASES = {"m", "mode", "default"};
    public static final String[] SHORT_ALIASES = {"s", "sh", "short", "shortcircuit"};

    private Operator operator = Operator.AND;
    private Tristate mode = UNDEFINED;
    private boolean shortCircuit = false;

    public LogicController(String name, int priority) {
        super(name, true, priority);
    }

    public LogicController(String name, boolean isEnabled, int priority, Operator operator, Tristate mode, boolean shortCircuit) {
        super(name, isEnabled, priority);
        this.operator = operator;
        this.mode = mode;
        this.shortCircuit = shortCircuit;
    }

    @Override
    public EventResult handle(@Nullable User user, FlagSet flags, ExtraContext extra) {
        return EventResult.of(operator.operate(this.handlers, mode, shortCircuit, user, flags, extra));
    }

    @Override
    public int maxLinks() {
        return operator == Operator.NOT ? 1 : -1;
    }

    @Override
    public String getShortTypeName() {
        return "Logic";
    }

    @Override
    public String getLongTypeName() {
        return getShortTypeName();
    }

    @Override
    public String getUniqueTypeString() {
        return "logic";
    }

    @Override
    public Text details(CommandSource source, String arguments) {
        Text.Builder builder = Text.builder();
        builder.append(Text.of(
                TextActions.suggestCommand("/foxguard md h " + this.name + " operator "),
                TextActions.showText(Text.of(Text.of("Click to change operator"))),
                TextColors.GOLD, "Operator: ",
                operator.color, operator.toString()
        ));
        builder.append(Text.builder()
                .append(Text.of(TextColors.GOLD, "\nMode: "))
                .append(FGUtil.readableTristateText(mode))
                .onClick(TextActions.suggestCommand("/foxguard md h " + this.name + " mode "))
                .onHover(TextActions.showText(Text.of(Text.of("Click to change mode"))))
                .build());
        builder.append(Text.builder()
                .append(Text.of(TextColors.GOLD, "\nShort Circuit: "))
                .append(Text.of(FCPUtil.readableBooleanText(shortCircuit)))
                .onClick(TextActions.runCommand("/foxguard md h " + this.name + " shortCircuit " + !shortCircuit))
                .onHover(TextActions.showText(Text.of(Text.of("Click to toggle short circuit"))))
                .build());
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
            if (isIn(OPERATOR_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    Operator op = Operator.from(parse.args[1]);
                    if (op != null) {
                        operator = op;
                        return ProcessResult.of(true, Text.of(GREEN, "Successfully set operator to ", op.color, op.toString(), GREEN, "!"));
                    } else
                        return ProcessResult.of(false, Text.of("\"" + parse.args[1] + "\" is not a valid operator!"));
                } else return ProcessResult.of(false, Text.of("You must specify an operator!"));
            } else if (isIn(MODE_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    Tristate tristate = tristateFrom(parse.args[1]);
                    if (tristate != null) {
                        this.mode = tristate;
                        return ProcessResult.of(true, Text.builder()
                                .append(Text.of(GREEN, "Successfully set mode to "))
                                .append(FCPUtil.readableTristateText(tristate))
                                .append(Text.of("!"))
                                .build());
                    } else
                        return ProcessResult.of(false, Text.of("\"" + parse.args[1] + "\" is not a valid tristate value!"));
                } else return ProcessResult.of(false, Text.of("You must specify a tristate value!"));
            } else if (isIn(SHORT_ALIASES, parse.args[0])) {
                if (parse.args.length > 1) {
                    boolean bool;
                    if (isIn(TRUE_ALIASES, parse.args[1])) {
                        bool = true;
                    } else if (isIn(FALSE_ALIASES, parse.args[1])) {
                        bool = false;
                    } else
                        return ProcessResult.of(false, Text.of("\"" + parse.args[1] + "\" is not a valid boolean value!"));
                    this.shortCircuit = bool;
                    return ProcessResult.of(true, Text.builder()
                            .append(Text.of(GREEN, "Successfully set mode to "))
                            .append(FCPUtil.readableBooleanText(bool))
                            .append(Text.of("!"))
                            .build());

                } else return ProcessResult.of(false, Text.of("You must specify a boolean!"));
            }
        } else return ProcessResult.of(false, Text.of("You must specify an option!"));
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
        AdvCmdParser.ParseResult parse = AdvCmdParser.builder()
                .arguments(arguments)
                .excludeCurrent(true)
                .autoCloseQuotes(true)
                .parse();
        if (parse.current.type == AdvCmdParser.CurrentElement.ElementType.ARGUMENT) {
            if (parse.current.index == 0) {
                return ImmutableList.of("operator", "mode", "short").stream()
                        .filter(new StartsWithPredicate(parse.current.token))
                        .map(args -> parse.current.prefix + args)
                        .collect(GuavaCollectors.toImmutableList());
            } else if (parse.current.index == 1) {
                if (isIn(OPERATOR_ALIASES, parse.args[0])) {
                    return Arrays.stream(Operator.values())
                            .map(Enum::name)
                            .map(String::toLowerCase)
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(MODE_ALIASES, parse.args[0])) {
                    return ImmutableList.of("allow", "deny", "pass").stream()
                            .filter(new StartsWithPredicate(parse.current.token))
                            .map(args -> parse.current.prefix + args)
                            .collect(GuavaCollectors.toImmutableList());
                } else if (isIn(SHORT_ALIASES, parse.args[0])) {
                    return ImmutableList.of("true", "false").stream()
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
        saveLinks(directory);
        Path configFile = directory.resolve("settings.cfg");
        CommentedConfigurationNode root;
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(configFile).build();
        if (Files.exists(configFile)) {
            try {
                root = loader.load();
            } catch (IOException e) {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        } else {
            root = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        root.getNode("operator").setValue(operator.name());
        root.getNode("mode").setValue(mode.name());
        root.getNode("shortCircuit").setValue(shortCircuit);
        try {
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum Operator {
        AND(GREEN) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (shortCircuit && state == FALSE) return FALSE;
                }
                return state;
            }
        },
        OR(AQUA) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (shortCircuit && state == TRUE) return TRUE;
                }
                return state;
            }
        },
        XOR(LIGHT_PURPLE) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = XORMatrix[state.ordinal()][ts.ordinal()];
                }
                return state;
            }
        },
        NOT(RED) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                if (handlers.size() > 0) {
                    Tristate state = handlers.get(0).handle(user, flags, extra).getState();
                    if (state == UNDEFINED) state = mode;
                    if (state == TRUE) state = FALSE;
                    else if (state == FALSE) state = TRUE;
                    return state;
                } else return UNDEFINED;
            }
        },
        NAND(DARK_GREEN) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (shortCircuit && state == FALSE) break;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        NOR(DARK_AQUA) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (shortCircuit && state == TRUE) break;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        XNOR(DARK_PURPLE) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, FlagSet flags, ExtraContext extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flags, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = XORMatrix[state.ordinal()][ts.ordinal()];
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        IGNORE(YELLOW) {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, @Nullable User user, FlagSet flags, ExtraContext extra) {
                for (IHandler handler : handlers) {
                    handler.handle(user, flags, extra);
                }
                return mode;
            }
        };

        private static Tristate[][] XORMatrix = {
                {FALSE, TRUE, TRUE},
                {TRUE, FALSE, FALSE},
                {TRUE, FALSE, UNDEFINED}
        };

        private final TextColor color;

        Operator(TextColor color) {
            this.color = color;
        }

        public abstract Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, @Nullable User user, FlagSet flags, ExtraContext extra);

        public static Operator from(String name) {
            for (Operator op : values()) {
                if (op.name().equalsIgnoreCase(name)) return op;
            }
            return null;
        }
    }

    public static final class Factory implements IControllerFactory {

        public static final String[] LOGIC_ALIASES = {"logic", "logical"};

        @Override
        public IController create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            return new LogicController(name, priority);
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type, @Nullable Location<World> targetPosition) throws CommandException {
            return ImmutableList.of();
        }

        @Override
        public IController create(Path directory, String name, int priority, boolean isEnabled) {
            Path configFile = directory.resolve("settings.cfg");
            CommentedConfigurationNode root;
            ConfigurationLoader<CommentedConfigurationNode> loader =
                    HoconConfigurationLoader.builder().setPath(configFile).build();
            if (Files.exists(configFile)) {
                try {
                    root = loader.load();
                } catch (IOException e) {
                    root = loader.createEmptyNode(ConfigurationOptions.defaults());
                }
            } else {
                root = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
            Operator operator = Operator.from(root.getNode("operator").getString(""));
            if (operator == null) operator = Operator.AND;
            String modeName = root.getNode("mode").getString("");
            Tristate mode;
            if (isIn(TRUE_ALIASES, modeName)) mode = TRUE;
            else if (isIn(FALSE_ALIASES, modeName)) mode = FALSE;
            else mode = UNDEFINED;
            boolean shortCircuit = root.getNode("shortCircuit").getBoolean(false);
            return new LogicController(name, isEnabled, priority, operator, mode, shortCircuit);
        }

        @Override
        public String[] getAliases() {
            return LOGIC_ALIASES;
        }

        @Override
        public String getType() {
            return "logic";
        }

        @Override
        public String getPrimaryAlias() {
            return "logic";
        }
    }
}
