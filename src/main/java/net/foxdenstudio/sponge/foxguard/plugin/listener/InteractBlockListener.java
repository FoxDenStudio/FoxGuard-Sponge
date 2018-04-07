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

import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGuardObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.*;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public class InteractBlockListener implements EventListener<InteractBlockEvent> {

    private static final FlagBitSet BASE_FLAG_SET = new FlagBitSet(ROOT, DEBUFF, INTERACT, BLOCK);

    @Override
    public void handle(InteractBlockEvent event) throws Exception {
        if (event.isCancelled()) return;
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }

        FlagBitSet flags = BASE_FLAG_SET.clone();
        BlockSnapshot block = event.getTargetBlock();
        if (block.getState().getType().equals(BlockTypes.AIR)) return;
        World world = block.getLocation().get().getExtent();
        Vector3i pos = block.getPosition();
        if (event instanceof InteractBlockEvent.Primary) {
            flags.set(PRIMARY);
            if (event instanceof InteractBlockEvent.Primary.MainHand) flags.set(MAIN);
            else if (event instanceof InteractBlockEvent.Primary.OffHand) flags.set(OFF);
        } else if (event instanceof InteractBlockEvent.Secondary) {
            flags.set(SECONDARY);
            if (event instanceof InteractBlockEvent.Secondary.MainHand) flags.set(MAIN);
            else if (event instanceof InteractBlockEvent.Secondary.OffHand) flags.set(OFF);
        }


        Set<IHandler> handlerSet = new HashSet<>();
        FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                .filter(region -> region.contains(pos, world))
                .forEach(region -> region.getLinks().stream()
                        .filter(IGuardObject::isEnabled)
                        .filter(handler -> !handlerSet.contains(handler))
                        .forEach(handlerSet::add));

        if (handlerSet.isEmpty()) {
            FoxGuardMain.instance().getLogger().error("Handlers list is empty for event: " + event);
            return;
        }

        List<IHandler> handlerList = new ArrayList<>(handlerSet);
        handlerList.sort(IHandler.PRIORITY);
        int currPriority = handlerList.get(0).getPriority();
        Tristate flagState = UNDEFINED;
        for (IHandler handler : handlerList) {
            if (handler.getPriority() < currPriority && flagState != UNDEFINED) {
                break;
            }
            flagState = flagState.and(handler.handle(user, flags, ExtraContext.of(event)).getState());
            currPriority = handler.getPriority();
        }
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
