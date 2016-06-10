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
import net.foxdenstudio.sponge.foxcore.common.FCUtil;
import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.flag.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.object.factory.IControllerFactory;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxcore.plugin.util.Aliases.*;
import static org.spongepowered.api.util.Tristate.*;

/**
 * Created by Fox on 3/27/2016.
 */
public class LogicController extends ControllerBase {

    private Operator operator = Operator.AND;
    private Tristate mode = UNDEFINED;
    private boolean shortCircuit = false;

    public LogicController(String name, int priority) {
        super(name, priority);
    }

    public LogicController(String name, int priority, Operator operator, Tristate mode, boolean shortCircuit) {
        super(name, priority);
        this.operator = operator;
        this.mode = mode;
        this.shortCircuit = shortCircuit;
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Optional<Event> event, Object... extra) {
        return EventResult.of(operator.operate(this.handlers, mode, shortCircuit, user, flag, event, extra));
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
        builder.append(Text.of(TextColors.GOLD, "Operator: "));
        builder.append(Text.of(TextColors.RESET, operator.toString()));
        builder.append(Text.of(TextColors.GOLD, "\nMode: "));
        builder.append(FGUtil.readableTristateText(mode));
        builder.append(Text.of(TextColors.GOLD, "\nShort Circuit: "));
        builder.append(Text.of(TextColors.RESET, shortCircuit));
        return builder.build();
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return null;
    }

    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return ProcessResult.failure();
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return ImmutableList.of();
    }


    @Override
    public void save(Path directory) {
        try (DB linksDB = DBMaker.fileDB(directory.resolve("links.db").normalize().toString()).make()) {
            List<String> linksList = linksDB.indexTreeList("links", Serializer.STRING).createOrOpen();
            linksList.clear();
            handlers.stream().map(IFGObject::getName).forEach(linksList::add);
        }
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

    @Override
    public void loadLinks(Path directory) {
        try (DB linksDB = DBMaker.fileDB(directory.resolve("links.db").normalize().toString()).make()) {
            List<String> linksList = linksDB.indexTreeList("links", Serializer.STRING).createOrOpen();
            handlers.clear();
            linksList.stream()
                    .filter(name -> !this.name.equalsIgnoreCase(name))
                    .map(name -> FGManager.getInstance().gethandler(name))
                    .filter(handler -> handler != null)
                    .forEach(handlers::add);

        }
    }

    private enum Operator {
        AND {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (shortCircuit && state == FALSE) return FALSE;
                }
                return state;
            }
        },
        OR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (shortCircuit && state == TRUE) return TRUE;
                }
                return state;
            }
        },
        XOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = XORMatrix[state.ordinal()][ts.ordinal()];
                }
                return state;
            }
        },
        NOT {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                if (handlers.size() > 0) {
                    Tristate state = handlers.get(0).handle(user, flag, event, extra).getState();
                    if (state == UNDEFINED) state = mode;
                    if (state == TRUE) state = FALSE;
                    else if (state == FALSE) state = TRUE;
                    return state;
                } else return UNDEFINED;
            }
        },
        NAND {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (shortCircuit && state == FALSE) break;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        NOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (shortCircuit && state == TRUE) break;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        XNOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event, extra).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = XORMatrix[state.ordinal()][ts.ordinal()];
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        };

        private static Tristate[][] XORMatrix = {
                {FALSE, TRUE, TRUE},
                {TRUE, FALSE, FALSE},
                {TRUE, FALSE, UNDEFINED}
        };

        public abstract Tristate operate(List<IHandler> handlers, Tristate mode, boolean shortCircuit, User user, IFlag flag, Optional<Event> event, Object... extra);

        public static Operator from(String name) {
            for (Operator op : values()) {
                if (op.name().equalsIgnoreCase(name)) return op;
            }
            return null;
        }
    }

    public static final class Factory implements IControllerFactory {

        public static final String[] LOGIC_ALIASES = {"logic", "logical", "boolean"};

        @Override
        public IController create(String name, int priority, String arguments, CommandSource source) throws CommandException {
            return new LogicController(name, priority);
        }

        @Override
        public List<String> createSuggestions(CommandSource source, String arguments, String type) throws CommandException {
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
            try {
                loader.save(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new LogicController(name, priority, operator, mode, shortCircuit);
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
