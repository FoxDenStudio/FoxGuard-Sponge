package net.foxdenstudio.sponge.foxguard.plugin.controller.util;

import net.foxdenstudio.sponge.foxguard.plugin.IFlag;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.util.Optional;

public class HandlerWrapper {

    public static final HandlerWrapper ALLOW = new HandlerWrapper(Tristate.TRUE);
    public static final HandlerWrapper PASSTHROUGH = new HandlerWrapper(Tristate.UNDEFINED);
    public static final HandlerWrapper DENY = new HandlerWrapper(Tristate.FALSE);

    public final Type type;
    public final Tristate tristate;
    public final IHandler handler;

    private HandlerWrapper(Type type, Tristate tristate, IHandler handler) {
        this.type = type;
        this.tristate = tristate;
        this.handler = handler;
    }

    public HandlerWrapper(IHandler handler) {
        this(Type.WRAPPER, null, handler);
    }

    private HandlerWrapper(Tristate state) {
        this(Type.CONSTANT, state, null);
    }

    public EventResult handle(@Nullable User user, IFlag flag, Optional<Event> event, Object... extra) {
        if (type == Type.WRAPPER) {
            return handler.handle(user, flag, event, extra);
        } else {
            return EventResult.of(tristate);
        }
    }

    public enum Type {
        CONSTANT, WRAPPER
    }
}
