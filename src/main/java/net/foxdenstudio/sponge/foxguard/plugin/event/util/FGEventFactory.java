package net.foxdenstudio.sponge.foxguard.plugin.event.util;

import net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateEvent;
import net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateObjectEvent;
import net.foxdenstudio.sponge.foxguard.plugin.event.FoxGuardEvent;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.event.SpongeEventFactoryUtils;
import org.spongepowered.api.event.cause.Cause;

import java.util.HashMap;

/**
 * Created by Fox on 7/9/2016.
 */
public class FGEventFactory {
    /**
     * AUTOMATICALLY GENERATED, DO NOT EDIT.
     * Creates a new instance of
     * {@link net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateEvent}.
     *
     * @param cause The cause
     * @return A new f g update event
     */
    public static FGUpdateEvent createFGUpdateEvent(Cause cause) {
        HashMap<String, Object> values = new HashMap<>();
        values.put("cause", cause);
        return SpongeEventFactoryUtils.createEventImpl(FGUpdateEvent.class, values);
    }

    /**
     * AUTOMATICALLY GENERATED, DO NOT EDIT.
     * Creates a new instance of
     * {@link net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateObjectEvent}.
     *
     * @param cause  The cause
     * @param target The target
     * @return A new f g update object event
     */
    public static FGUpdateObjectEvent createFGUpdateObjectEvent(Cause cause, IFGObject target) {
        HashMap<String, Object> values = new HashMap<>();
        values.put("cause", cause);
        values.put("target", target);
        return SpongeEventFactoryUtils.createEventImpl(FGUpdateObjectEvent.class, values);
    }

    /**
     * AUTOMATICALLY GENERATED, DO NOT EDIT.
     * Creates a new instance of
     * {@link net.foxdenstudio.sponge.foxguard.plugin.event.FoxGuardEvent}.
     *
     * @param cause The cause
     * @return A new fox guard event
     */
    public static FoxGuardEvent createFoxGuardEvent(Cause cause) {
        HashMap<String, Object> values = new HashMap<>();
        values.put("cause", cause);
        return SpongeEventFactoryUtils.createEventImpl(FoxGuardEvent.class, values);
    }
}

