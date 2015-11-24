/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/
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

import com.flowpowered.math.vector.Vector3i;
import net.gravityfox.foxguard.util.DebugHelper;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;
import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.flagsets.IFlagSet;
import net.gravityfox.foxguard.flagsets.util.ActiveFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 * Project: foxguard
 */
public class BlockEventListener implements EventListener<ChangeBlockEvent> {

    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        if(event.isCancelled()) return;
        User user;
        if (event.getCause().any(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().any(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }
        //DebugHelper.printBlockEvent(event);
        ActiveFlags typeFlag;
        if (event instanceof ChangeBlockEvent.Modify) typeFlag = ActiveFlags.BLOCK_MODIFY;
        else if (event instanceof ChangeBlockEvent.Fluid) typeFlag = ActiveFlags.FLUID;
        else if (event instanceof ChangeBlockEvent.Break) typeFlag = ActiveFlags.BLOCK_BREAK;
        else if (event instanceof ChangeBlockEvent.Place) typeFlag = ActiveFlags.BLOCK_PLACE;
        else return;


        //FoxGuardMain.getInstance().getLogger().info(player.getName());

        List<IFlagSet> flagSetList = new ArrayList<>();
        World world = event.getTargetWorld();

        for (Transaction<BlockSnapshot> trans : event.getTransactions()) {
            Vector3i loc = trans.getOriginal().getLocation().get().getBlockPosition();
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
            flagState = flagState.and(flagSet.hasPermission(user, typeFlag, event));
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
