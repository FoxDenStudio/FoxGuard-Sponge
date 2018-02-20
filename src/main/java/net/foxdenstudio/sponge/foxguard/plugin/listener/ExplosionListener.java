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
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.listener.util.EventResult;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;

public class ExplosionListener implements EventListener<ExplosionEvent> {

    private static final FlagBitSet FLAG_SET = new FlagBitSet(ROOT, DEBUFF, EXPLOSION);


    @Override
    public void handle(ExplosionEvent event) throws Exception {
        if (!(event instanceof Cancellable) || ((Cancellable) event).isCancelled()) return;
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            // Duct tape:
            Optional<Explosive> explosiveOptional = event.getExplosion().getSourceExplosive();
            if (explosiveOptional.isPresent()) {
                Explosive explosive = explosiveOptional.get();
                UUID uuid;
                Optional<UUID> notifierOptional = explosive.getNotifier();
                uuid = notifierOptional.orElseGet(() -> explosive.getCreator().orElse(null));
                if (uuid != null) {
                    UserStorageService storageService = FoxGuardMain.instance().getUserStorage();
                    user = storageService.get(uuid).orElse(null);
                } else user = null;
            } else user = null;
        }

        FlagBitSet flags = FLAG_SET.clone();

        Set<IHandler> handlerSet = new HashSet<>();
        if (event instanceof ExplosionEvent.Post) {
            flags.set(POST);
            flags.set(BLOCK);
            flags.set(CHANGE);

            ExplosionEvent.Post postEvent = (ExplosionEvent.Post) event;
            FGManager.getInstance().getRegionsAtMultiLocI(
                    postEvent.getTransactions().stream()
                            .map(trans -> trans.getOriginal().getLocation().get())
                            .collect(Collectors.toList())
            ).forEach(region -> region.getLinks().stream()
                    .filter(IFGObject::isEnabled)
                    .forEach(handlerSet::add));
        } else if (event instanceof ExplosionEvent.Detonate) {
            flags.set(DETONATE);

            ExplosionEvent.Detonate detonateEvent = ((ExplosionEvent.Detonate) event);

            List<Location<World>> locations = detonateEvent.getAffectedLocations();
            if (locations.isEmpty()) return;

            FGManager.getInstance().getRegionsAtMultiLocI(locations)
                    .forEach(region -> region.getLinks().stream()
                            .filter(IFGObject::isEnabled)
                            .forEach(handlerSet::add));

        } else if (event instanceof ExplosionEvent.Pre) {
            flags.set(PRE);
            Location<World> loc = event.getExplosion().getLocation();
            Vector3d pos = loc.getPosition();
            World world = loc.getExtent();
            FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                    .filter(region -> region.contains(pos, world))
                    .forEach(region -> region.getLinks().stream()
                            .filter(IFGObject::isEnabled)
                            .forEach(handlerSet::add));
        }
        Tristate flagState = Tristate.UNDEFINED;

        if (handlerSet.isEmpty()) {
            FoxGuardMain.instance().getLogger().error("Handlers list is empty for event: " + event);
            return;
        }

        List<IHandler> handlerList = new ArrayList<>(handlerSet);
        handlerList.sort(IHandler.PRIORITY);

        int currPriority = handlerList.get(0).getPriority();
        for (IHandler handler : handlerList) {
            if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            EventResult result = handler.handle(user, flags, ExtraContext.of(event));
            if (result != null) {
                flagState = flagState.and(result.getState());
            } else {
                FoxGuardMain.instance().getLogger().error("Handler \"" + handler.getName() + "\" of type \"" + handler.getUniqueTypeString() + "\" returned null!");
            }
            currPriority = handler.getPriority();
        }


        if (flagState == Tristate.FALSE) {
            if (user instanceof Player)
                ((Player) user).sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
            ((Cancellable) event).setCancelled(true);
        } else {
            //makes sure that handlers are unable to cancel the event directly.
            ((Cancellable) event).setCancelled(false);
        }
    }
}
