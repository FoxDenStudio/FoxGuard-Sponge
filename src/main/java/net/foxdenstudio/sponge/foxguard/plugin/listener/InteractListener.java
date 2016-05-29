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
import net.foxdenstudio.sponge.foxguard.plugin.flag.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InteractListener implements EventListener<InteractEvent> {

    @Override
    public void handle(InteractEvent event) throws Exception {
        if (event.isCancelled()) return;
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }

        World world = null;
        Flag typeFlag = null;
        Vector3d loc = null;
        if (event instanceof InteractEntityEvent) {
            world = ((InteractEntityEvent) event).getTargetEntity().getWorld();
            loc = ((InteractEntityEvent) event).getTargetEntity().getLocation().getPosition();
            if (event instanceof InteractEntityEvent.Primary) {
                typeFlag = Flag.ENTITY_INTERACT_PRIMARY;
                if (((InteractEntityEvent.Primary) event).getTargetEntity() instanceof Player)
                    typeFlag = Flag.PLAYER_INTERACT_PRIMARY;
            } else if (event instanceof InteractEntityEvent.Secondary) {
                typeFlag = Flag.ENTITY_INTERACT_SECONDARY;
                if (((InteractEntityEvent.Secondary) event).getTargetEntity() instanceof Player)
                    typeFlag = Flag.PLAYER_INTERACT_SECONDARY;
            }
        } else if (event instanceof InteractBlockEvent) {
            if(((InteractBlockEvent) event).getTargetBlock().getState().getType().equals(BlockTypes.AIR)) return;
            world = ((InteractBlockEvent) event).getTargetBlock().getLocation().get().getExtent();
            loc = ((InteractBlockEvent) event).getTargetBlock().getPosition().toDouble();
            if (event instanceof InteractBlockEvent.Primary) typeFlag = Flag.BLOCK_INTERACT_PRIMARY;
            else if (event instanceof InteractBlockEvent.Secondary) typeFlag = Flag.BLOCK_INTERACT_SECONDARY;
        }
        if (typeFlag == null) return;
        List<IHandler> handlerList = new ArrayList<>();
        Vector3i chunk = new Vector3i(
                GenericMath.floor(loc.getX() / 16.0),
                GenericMath.floor(loc.getY() / 16.0),
                GenericMath.floor(loc.getZ() / 16.0));
        final Vector3d finalLoc = loc;
        final World finalWorld = world;
        FGManager.getInstance().getAllRegions(world, chunk).stream()
                .filter(region -> region.contains(finalLoc, finalWorld))
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !handlerList.contains(handler))
                        .forEach(handlerList::add));

        Collections.sort(handlerList);
        int currPriority = handlerList.get(0).getPriority();
        Tristate flagState = Tristate.UNDEFINED;
        for (IHandler handler : handlerList) {
            if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            flagState = flagState.and(handler.handle(user, typeFlag, Optional.of(event)).getState());
            currPriority = handler.getPriority();
        }
        flagState = typeFlag.resolve(flagState);
        if (flagState == Tristate.FALSE) {
            if (user instanceof Player)
                ((Player) user).sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
            event.setCancelled(true);
        } else {
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }
}
