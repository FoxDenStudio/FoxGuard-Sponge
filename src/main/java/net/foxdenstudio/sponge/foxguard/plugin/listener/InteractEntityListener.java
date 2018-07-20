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
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.FGListenerUtil;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.*;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public class InteractEntityListener implements EventListener<InteractEntityEvent> {

    private static final boolean[] BASE_FLAG_SET = FlagSet.arrayFromFlags(ROOT, DEBUFF, INTERACT, ENTITY);

    @Override
    public void handle(InteractEntityEvent event) throws Exception {
        if (event.isCancelled()) return;

        World world = event.getTargetEntity().getWorld();
        Vector3d pos = event.getTargetEntity().getLocation().getPosition();

        Set<IHandler> handlerSet = new HashSet<>();
        FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                .filter(region -> region.contains(pos, world))
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .forEach(handlerSet::add));

        if (handlerSet.isEmpty()) {
            FoxGuardMain.instance().getLogger().warn("Handler set is empty for interact block listener!");
            return;
        }

        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }

        boolean[] flags = BASE_FLAG_SET.clone();
        if (event instanceof InteractEntityEvent.Primary) {
            flags[PRIMARY.id] = true;
            if (event instanceof InteractEntityEvent.Primary.MainHand) flags[MAIN.id] = true;
            else if (event instanceof InteractEntityEvent.Primary.OffHand) flags[OFF.id] = true;
        } else if (event instanceof InteractEntityEvent.Secondary) {
            flags[SECONDARY.id] = true;
            if (event instanceof InteractEntityEvent.Secondary.MainHand) flags[MAIN.id] = true;
            else if (event instanceof InteractEntityEvent.Secondary.OffHand) flags[OFF.id] = true;
        }
        Entity entity = event.getTargetEntity();
        FGListenerUtil.applyEntityFlags(entity, flags);
        FlagSet flagSet = new FlagSet(flags);

        List<IHandler> handlerList = new ArrayList<>(handlerSet);
        Collections.sort(handlerList);
        int currPriority = handlerList.get(0).getPriority();
        Tristate flagState = UNDEFINED;
        for (IHandler handler : handlerList) {
            if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                break;
            }
            //flagState = flagState.and(handler.handle(user, typeFlag, Optional.of(event)).getState());
            flagState = flagState.and(handler.handle(user, flagSet, ExtraContext.of(event)).getState());
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
    }
}
