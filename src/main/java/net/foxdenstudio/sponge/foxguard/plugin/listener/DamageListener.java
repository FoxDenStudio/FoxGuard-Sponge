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

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.foxdenstudio.sponge.foxguard.plugin.Flag.*;

/**
 * Created by Fox on 5/9/2016.
 */
public class DamageListener implements EventListener<DamageEntityEvent> {

    @Override
    public void handle(DamageEntityEvent event) throws Exception {
        if (event.isCancelled()) return;
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }

        World world = event.getTargetEntity().getWorld();
        Vector3d loc = event.getTargetEntity().getLocation().getPosition();
        Flag flag, secondaryFlag = null;
        Entity entity = event.getTargetEntity();

        if (entity instanceof Living) {
            if (entity instanceof Agent) {
                //noinspection ConstantConditions
                if (entity instanceof Hostile) {
                    flag = DAMAGE_MOB_HOSTILE;
                    if (event.willCauseDeath()) {
                        secondaryFlag = KILL_MOB_HOSTILE;
                    }
                } else {
                    flag = DAMAGE_MOB_PASSIVE;
                    if (event.willCauseDeath()) {
                        secondaryFlag = KILL_MOB_PASSIVE;
                    }
                }
            } else if (entity instanceof Player) {
                flag = DAMAGE_PLAYER;
                if (event.willCauseDeath()) {
                    secondaryFlag = KILL_PLAYER;
                }
            } else {
                flag = DAMAGE_LIVING;
                if (event.willCauseDeath()) {
                    secondaryFlag = KILL_LIVING;
                }
            }
        } else {
            flag = DAMAGE_ENTITY;
        }

        List<IHandler> handlerList = new ArrayList<>();
        final Vector3d finalLoc = loc;
        final World finalWorld = world;
        FGManager.getInstance().getAllRegions(world, new Vector3i(
                GenericMath.floor(loc.getX() / 16.0),
                GenericMath.floor(loc.getY() / 16.0),
                GenericMath.floor(loc.getZ() / 16.0))).stream()
                .filter(region -> region.contains(finalLoc, finalWorld))
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !handlerList.contains(handler))
                        .forEach(handlerList::add));

        Collections.sort(handlerList);
        int currPriority;
        Tristate flagState = Tristate.UNDEFINED;
        boolean invincible = false;
        if (entity instanceof Player) {
            currPriority = handlerList.get(0).getPriority();
            for (IHandler handler : handlerList) {
                if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                    break;
                }
                flagState = flagState.and(handler.handle((Player) entity, INVINCIBLE, Optional.of(event)).getState());
                currPriority = handler.getPriority();
            }
            flagState = INVINCIBLE.resolve(flagState);
            if (flagState == Tristate.TRUE) {
                invincible = true;
                flagState = Tristate.FALSE;
            }
        }
        if (!invincible) {
            currPriority = handlerList.get(0).getPriority();
            flagState = Tristate.UNDEFINED;
            for (IHandler handler : handlerList) {
                if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                    break;
                }
                flagState = flagState.and(handler.handle(user, flag, Optional.of(event)).getState());
                currPriority = handler.getPriority();
            }
            flagState = flag.resolve(flagState);
        }
        if (flagState == Tristate.FALSE) {
            if (user instanceof Player && !invincible) {
                ((Player) user).sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
            }
            event.setCancelled(true);
        } else {
            if (event.willCauseDeath()) {
                flagState = Tristate.UNDEFINED;
                invincible = false;
                if (entity instanceof Player) {
                    currPriority = handlerList.get(0).getPriority();
                    for (IHandler handler : handlerList) {
                        if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                            break;
                        }
                        flagState = flagState.and(handler.handle((Player) entity, UNDYING, Optional.of(event)).getState());
                        currPriority = handler.getPriority();
                    }
                    flagState = UNDYING.resolve(flagState);
                    if (flagState == Tristate.TRUE) {
                        invincible = true;
                        flagState = Tristate.FALSE;
                    }
                }
                if (!invincible) {
                    currPriority = handlerList.get(0).getPriority();
                    flagState = Tristate.UNDEFINED;
                    for (IHandler handler : handlerList) {
                        if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                            break;
                        }
                        flagState = flagState.and(handler.handle(user, secondaryFlag, Optional.of(event)).getState());
                        currPriority = handler.getPriority();
                    }
                    flagState = flag.resolve(flagState);
                }
                if (flagState == Tristate.FALSE) {
                    DamageModifier.Builder builder = DamageModifier.builder();
                    builder.type(DamageModifierTypes.ABSORPTION);
                    builder.cause(FoxGuardMain.getCause());
                    event.setDamage(builder.build(), damage -> ((Living) event.getTargetEntity()).getHealthData().health().get() - damage - 1);
                    if (user instanceof Player && !invincible)
                        ((Player) user).sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
                }
            }
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }

}
