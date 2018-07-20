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

package net.foxdenstudio.sponge.foxguard.plugin.listener;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EntityFlagCalculator;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.*;

/**
 * Created by Fox on 5/9/2016.
 */
public class DamageListener implements EventListener<DamageEntityEvent> {

    private static final EntityFlagCalculator ENTITY_FLAG_CALCULATOR = EntityFlagCalculator.getInstance();
    private static final boolean[] BASE_FLAGS_SOURCE = FlagSet.arrayFromFlags(ROOT, DEBUFF, DAMAGE, ENTITY);
    private static final boolean[] INVINCIBLE_FLAGS = FlagSet.arrayFromFlags(ROOT, BUFF, INVINCIBLE);
    private static final FlagSet INVINCIBLE_FLAG_SET = new FlagSet(INVINCIBLE_FLAGS);
    private static final boolean[] UNDYING_FLAGS = FlagSet.arrayFromFlags(ROOT, BUFF, INVINCIBLE, UNDYING);
    private static final FlagSet UNDYING_FLAG_SET = new FlagSet(UNDYING_FLAGS);

    @SuppressWarnings("Duplicates")
    @Override
    public void handle(DamageEntityEvent event) throws Exception {
        if (event.isCancelled()) return;

        World world = event.getTargetEntity().getWorld();
        Vector3d pos = event.getTargetEntity().getLocation().getPosition();
        Entity entity = event.getTargetEntity();

        List<IHandler> handlerList = new ArrayList<>();
        FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                .filter(region -> region.contains(pos, world))
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !handlerList.contains(handler))
                        .forEach(handlerList::add));

        Collections.sort(handlerList);
        int currPriority;
        Tristate flagState = UNDEFINED;
        boolean invincible = false;
        if (entity instanceof Player) {
            currPriority = handlerList.get(0).getPriority();
            for (IHandler handler : handlerList) {
                if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                    break;
                }
                flagState = flagState.and(handler.handle((Player) entity, INVINCIBLE_FLAG_SET, ExtraContext.of(event)).getState());
                currPriority = handler.getPriority();
            }
//            if(flagState == UNDEFINED) flagState = FALSE;
            if (flagState == TRUE) {
                invincible = true;
                flagState = FALSE;
            }
        }
        boolean[] flags = null;
        FlagSet flagSet;
        Player player = null;
        if (!invincible) {
            player = getPlayerCause(event.getCause());

            flags = BASE_FLAGS_SOURCE.clone();

            //FGListenerUtil.applyEntityFlags(entity, flags);
            ENTITY_FLAG_CALCULATOR.applyEntityFlags(ImmutableList.of(entity), flags);


            flagSet = new FlagSet(flags);
            currPriority = handlerList.get(0).getPriority();
            flagState = UNDEFINED;
            for (IHandler handler : handlerList) {
                if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                    break;
                }
                flagState = flagState.and(handler.handle(player, flagSet, ExtraContext.of(event)).getState());
                currPriority = handler.getPriority();
            }
//            if(flagState == UNDEFINED) flagState = TRUE;
        }
        if (flagState == FALSE) {
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
            }
            event.setCancelled(true);
        } else {
            if (event.willCauseDeath()) {
                flagState = UNDEFINED;
                invincible = false;
                if (entity instanceof Player) {
                    currPriority = handlerList.get(0).getPriority();
                    for (IHandler handler : handlerList) {
                        if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                            break;
                        }
                        flagState = flagState.and(handler.handle((Player) entity, UNDYING_FLAG_SET, ExtraContext.of(event)).getState());
                        currPriority = handler.getPriority();
                    }
//                    if(flagState == UNDEFINED) flagState = FALSE;
                    if (flagState == TRUE) {
                        invincible = true;
                        flagState = FALSE;
                    }
                }
                if (!invincible) {
                    flags = flags.clone();
                    flags[KILL.id] = true;
                    flagSet = new FlagSet(flags);

                    currPriority = handlerList.get(0).getPriority();
                    flagState = UNDEFINED;
                    for (IHandler handler : handlerList) {
                        if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                            break;
                        }
                        flagState = flagState.and(handler.handle(player, flagSet, ExtraContext.of(event)).getState());
                        currPriority = handler.getPriority();
                    }
//                    if(flagState == UNDEFINED) flagState = TRUE;
                }
                if (flagState == FALSE) {
                    DamageModifier.Builder builder = DamageModifier.builder();
                    builder.type(DamageModifierTypes.ABSORPTION);
                    builder.cause(FoxGuardMain.getCause());
                    event.setDamage(builder.build(), damage -> ((Living) event.getTargetEntity()).getHealthData().health().get() - damage - 1);
                    if (player != null && player.isOnline() && !invincible)
                        player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission to kill!"));
                }
            }
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }

    @Nullable
    private Player getPlayerCause(Cause cause) {
        List<EntityDamageSource> sources = cause.allOf(EntityDamageSource.class);
        for (EntityDamageSource source : sources) {
            Entity entity;
            entity = source.getSource();
            if (entity instanceof Player) {
                return (Player) entity;
            }
            if (source instanceof IndirectEntityDamageSource) {
                entity = ((IndirectEntityDamageSource) source).getIndirectSource();
                if (entity instanceof Player) {
                    return (Player) entity;
                }
            }
        }
        return null;
    }
}
