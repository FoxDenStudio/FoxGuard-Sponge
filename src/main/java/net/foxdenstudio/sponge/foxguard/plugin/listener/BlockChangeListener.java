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
import net.foxdenstudio.sponge.foxcore.plugin.command.CommandDebug;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.util.ExtraContext;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

public class BlockChangeListener implements EventListener<ChangeBlockEvent> {

    private static final FlagBitSet BASE_FLAG_SET = new FlagBitSet(ROOT, DEBUFF, BLOCK, CHANGE);

    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        if (event.isCancelled() || event instanceof ExplosionEvent || event.getTransactions().isEmpty()) return;
        for (Transaction<BlockSnapshot> tr : event.getTransactions()) {
            if (tr.getOriginal().getState().getType().equals(BlockTypes.DIRT)
                    && tr.getFinal().getState().getType().equals(BlockTypes.GRASS)
                    || tr.getOriginal().getState().getType().equals(BlockTypes.GRASS)
                    && tr.getFinal().getState().getType().equals(BlockTypes.DIRT)) return;
        }
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }
        //DebugHelper.printBlockEvent(event);
        /*FlagOld typeFlag;
        if (event instanceof ChangeBlockEvent.Modify) typeFlag = FlagOld.BLOCK_MODIFY;
        else if (event instanceof ChangeBlockEvent.Break) typeFlag = FlagOld.BLOCK_BREAK;
        else if (event instanceof ChangeBlockEvent.Place) typeFlag = FlagOld.BLOCK_PLACE;
        else if (event instanceof ChangeBlockEvent.Decay) typeFlag = FlagOld.BLOCK_DECAY;
        else if (event instanceof ChangeBlockEvent.Grow) typeFlag = FlagOld.BLOCK_GROW;
        else return;*/

        FlagBitSet flags = BASE_FLAG_SET.clone();

        if (event instanceof ChangeBlockEvent.Modify) flags.set(MODIFY);
        else if (event instanceof ChangeBlockEvent.Break) flags.set(BREAK);
        else if (event instanceof ChangeBlockEvent.Place) flags.set(PLACE);
        else if (event instanceof ChangeBlockEvent.Post) flags.set(POST);
        else if (event instanceof ChangeBlockEvent.Decay) flags.set(DECAY);
        else if (event instanceof ChangeBlockEvent.Grow) flags.set(GROW);

        //FoxGuardMain.instance().getLogger().info(player.getName());

        List<IHandler> handlerList;

//        System.err.println(event);
//        System.err.println(event.getClass());
//        System.err.println(event.getTransactions());
//        System.exit(1);

//        if (1 == 1) return;

        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        Set<IHandler> handlerSet = new HashSet<>();
        if (transactions.size() == 1) {
            final World world = transactions.get(0).getFinal().getLocation().get().getExtent();
            final Vector3i pos = transactions.get(0).getFinal().getLocation().get().getBlockPosition();
            FGManager.getInstance().getRegionsInChunkAtPos(world, pos).stream()
                    .filter(region -> region.contains(pos, world))
                    .forEach(region -> region.getHandlers().stream()
                            .filter(IFGObject::isEnabled)
                            .forEach(handlerSet::add));
        } else {
            final World world = transactions.get(0).getFinal().getLocation().get().getExtent();
            FGManager.getInstance().getRegionsAtMultiPosI(
                    world,
                    transactions.stream()
                            .map(trans -> trans.getFinal().getLocation().get().getBlockPosition())
                            .collect(Collectors.toList())
            ).forEach(region -> region.getHandlers().stream()
                    .filter(IFGObject::isEnabled)
                    .forEach(handlerSet::add));
        }
        handlerList = new ArrayList<>(handlerSet);
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
            if (user instanceof Player) {
                if (CommandDebug.instance().getDebug().get(user)) {
                    Vector3i vec = event.getTransactions().get(0).getOriginal().getPosition();
                    ((Player) user).sendMessage(Text.of("Block action denied at (" + vec.getX() + ", " + vec.getY() + ", " + vec.getZ() + ")"
                            + (event.getTransactions().size() > 1 ? " and " + (event.getTransactions().size() - 1) + " other positions" : "") + "!"));
                } else {
                    Player player = (Player) user;
                    Vector3i pos = player.getLocation().getPosition().toInt();
                    Response r = Response.NONE;
                    // Using the first transaction is good enough. No one cares if the tooltip is off a little.
                    int dist = event.getTransactions().get(0).getOriginal().getPosition().distanceSquared(pos);
                    if (dist < 64) {
                        r = Response.BASIC;
                    }
                    if (dist < 4096) {
                        r = Response.LOCATION;
                    }
                    if (r == Response.BASIC)
                        player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission!"));
                    else if (r == Response.LOCATION)
                        player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission! " +
                                event.getTransactions().get(0).getOriginal().getPosition() +
                                (event.getTransactions().size() > 1 ? "..." : "")));
                }
            }
            event.setCancelled(true);
        } else {
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }

    private enum Response {
        NONE, BASIC, LOCATION
    }

}
