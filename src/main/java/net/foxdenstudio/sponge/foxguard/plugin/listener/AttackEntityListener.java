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
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.FishHook;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public class AttackEntityListener implements EventListener<AttackEntityEvent> {

    private static final FlagBitSet BASE_FLAG_SET = new FlagBitSet(ROOT, DEBUFF, DAMAGE, ENTITY);

    @Override
    public void handle(AttackEntityEvent event) throws Exception {
        if (event.isCancelled()) return;

        event.getContext().get(EventContextKeys.OWNER).ifPresent(user -> {
            FlagBitSet flags = BASE_FLAG_SET.clone();
            World world = event.getTargetEntity().getWorld();
            Vector3d pos = event.getTargetEntity().getLocation().getPosition();
            Entity entity = event.getTargetEntity();
            if (entity instanceof Living) {
                flags.set(LIVING);
                if (entity instanceof Agent) {
                    flags.set(MOB);
                    if (entity instanceof Hostile) {
                        flags.set(HOSTILE);
                    } else if (entity instanceof Human) {
                        flags.set(HUMAN);
                    } else {
                        flags.set(PASSIVE);
                    }
                } else if (entity instanceof Player) {
                    flags.set(PLAYER);
                }

            } else if (entity instanceof Hanging) {
                flags.set(HANGING);
            }

            List<IHandler> handlerList = new ArrayList<>();
            FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                    .filter(region -> region.contains(pos, world))
                    .forEach(region -> region.getHandlers().stream()
                            .filter(IFGObject::isEnabled)
                            .filter(handler -> !handlerList.contains(handler))
                            .forEach(handlerList::add));

            Collections.sort(handlerList);
            int currPriority = handlerList.get(0).getPriority();
            Tristate flagState = UNDEFINED;
            for (IHandler handler : handlerList) {
                if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                    break;
                }
                //flagState = flagState.and(handler.handle(user, typeFlag, Optional.of(event)).getState());
                flagState = flagState.and(handler.handle(user, flags, ExtraContext.of(event)).getState());
                currPriority = handler.getPriority();
            }
            //        if(flagState == UNDEFINED) flagState = TRUE;
            if (flagState == FALSE) {
                if (user instanceof Player)
                    ((Player) user).sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
                event.setCancelled(true);
            } else {
                //makes sure that handlers are unable to cancel the event directly.
                event.setCancelled(false);
            }
        });
    }
}
