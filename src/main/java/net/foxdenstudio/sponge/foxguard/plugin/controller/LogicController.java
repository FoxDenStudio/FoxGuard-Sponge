package net.foxdenstudio.sponge.foxguard.plugin.controller;

import net.foxdenstudio.sponge.foxcore.plugin.command.util.ProcessResult;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

import static org.spongepowered.api.util.Tristate.*;

/**
 * Created by Fox on 3/27/2016.
 */
public class LogicController extends ControllerBase {

    LogicController(String name, int priority) {
        super(name, priority);
    }

    @Override
    public void loadLinks(Path directory) {

    }

    @Override
    public int maxLinks() {
        return -1;
    }

    @Override
    public EventResult handle(@Nullable User user, IFlag flag, Event event) {
        return null;
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
        return null;
    }

    @Override
    public List<String> detailsSuggestions(CommandSource source, String arguments) {
        return null;
    }

    @Override
    public void save(Path directory) {

    }


    @Override
    public ProcessResult modify(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public List<String> modifySuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    private enum Operation {
        OR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (state == TRUE) return TRUE;
                }
                return state;
            }
        },
        AND {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (state == FALSE) return FALSE;
                }
                return state;
            }
        },
        XOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = XORMatrix[state.ordinal()][ts.ordinal()];
                }
                return state;
            }
        },
        NOT {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                if (handlers.size() > 0) {
                    Tristate state = handlers.get(0).handle(user, flag, event).getState();
                    if (state == UNDEFINED) state = mode;
                    if (state == TRUE) state = FALSE;
                    else if (state == FALSE) state = TRUE;
                    return state;
                } else return UNDEFINED;
            }
        },
        NOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.or(ts);
                    if (state == TRUE) return TRUE;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        NAND {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
                    if (ts == UNDEFINED) ts = mode;
                    state = state.and(ts);
                    if (state == FALSE) return FALSE;
                }
                if (state == TRUE) state = FALSE;
                else if (state == FALSE) state = TRUE;
                return state;
            }
        },
        XNOR {
            @Override
            public Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event) {
                Tristate state = UNDEFINED;
                for (IHandler handler : handlers) {
                    Tristate ts = handler.handle(user, flag, event).getState();
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

        public abstract Tristate operate(List<IHandler> handlers, Tristate mode, User user, Flag flag, Event event);
    }
}
