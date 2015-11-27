/*
 *
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.listener;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.flagsets.util.Flags;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by Fox on 11/26/2015.
 * Project: SpongeForge
 */
public class SpawnEntityEventListener implements EventListener<SpawnEntityEvent> {

    @Override
    public void handle(SpawnEntityEvent event) throws Exception {
        if (event.isCancelled()) return;
        User user;
        if (event.getCause().any(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().any(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }

        Flags typeFlag = null;
        Entity oneEntity = event.getEntities().get(0);
        if (oneEntity instanceof Creature) typeFlag = Flags.SPAWN_MOB_PASSIVE;
        if (oneEntity instanceof Hostile) typeFlag = Flags.SPAWN_MOB_HOSTILE;
        if (typeFlag == null) return;

        List<IFlagSet> flagSetList = new ArrayList<>();
        World world = event.getTargetWorld();

        for (Entity entity : event.getEntities()) {
            Vector3d loc = entity.getLocation().getPosition();
            FGManager.getInstance().getRegionListAsStream(world).filter(region -> region.isInRegion(loc))
                    .forEach(region -> region.getFlagSets().stream()
                            .filter(flagSet -> !flagSetList.contains(flagSet))
                            .forEach(flagSetList::add));
        }
        Collections.sort(flagSetList);
        int currPriority = flagSetList.get(0).getPriority();
        Tristate flagState = Tristate.UNDEFINED;
        for (IFlagSet flagSet : flagSetList) {
            if (flagSet.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            flagState = flagState.and(flagSet.isAllowed(user, typeFlag, event));
            currPriority = flagSet.getPriority();
        }
        if (flagState == Tristate.FALSE) {
            if (user instanceof Player)
                ((Player) user).sendMessage(Texts.of("You don't have permission!"));
            event.setCancelled(true);
        } else {
            //makes sure that flagsets are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }

}
