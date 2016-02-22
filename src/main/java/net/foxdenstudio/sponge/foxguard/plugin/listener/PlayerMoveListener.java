package net.foxdenstudio.sponge.foxguard.plugin.listener;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxcore.plugin.util.CallbackHashMap;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.Flag;
import net.foxdenstudio.sponge.foxguard.plugin.event.FGUpdateEvent;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Fox on 1/4/2016.
 * Project: SpongeForge
 */
public class PlayerMoveListener implements EventListener<DisplaceEntityEvent> {

    private final LastWrapper EMPTY_LAST_WRAPPER = new LastWrapper(null, null);

    private final Map<Player, LastWrapper> last = new CallbackHashMap<>((key, map) -> EMPTY_LAST_WRAPPER);

    @Override
    public void handle(DisplaceEntityEvent event) throws Exception {
        if (event.isCancelled()) return;
        Player player;
        if (event.getTargetEntity().getPassenger().isPresent() && event.getTargetEntity().getPassenger().get() instanceof Player) {
            player = (Player) event.getTargetEntity().getPassenger().get();
        } else if (event instanceof DisplaceEntityEvent.TargetPlayer) {
            player = ((DisplaceEntityEvent.TargetPlayer) event).getTargetEntity();
        } else return;
        World world = event.getTargetEntity().getWorld();
        List<IHandler> fromList = last.get(player).list, toList = new ArrayList<>();
        Vector3d to = event.getToTransform().getPosition().add(0, 0.1, 0);
        if (fromList == null) {
            fromList = new ArrayList<>();
            final List<IHandler> temp = fromList;
            Vector3d from = event.getFromTransform().getPosition().add(0, 0.1, 0);
            FGManager.getInstance().getRegionList(world, new Vector3i(
                    GenericMath.floor(from.getX() / 16.0),
                    GenericMath.floor(from.getY() / 16.0),
                    GenericMath.floor(from.getZ() / 16.0))).stream()
                    .filter(region -> region.contains(from))
                    .filter(IFGObject::isEnabled)
                    .forEach(region -> region.getHandlers().stream()
                            .filter(IFGObject::isEnabled)
                            .filter(handler -> !temp.contains(handler))
                            .forEach(temp::add));
        }
        FGManager.getInstance().getRegionList(world, new Vector3i(
                GenericMath.floor(to.getX() / 16.0),
                GenericMath.floor(to.getY() / 16.0),
                GenericMath.floor(to.getZ() / 16.0))).stream()
                .filter(region -> region.contains(to))
                .filter(IFGObject::isEnabled)
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !toList.contains(handler))
                        .forEach(toList::add));

        //System.out.println(fromList);
        //System.out.println(toList);

        final List<IHandler> toComplete = new ArrayList<>(toList);

        final List<IHandler> temp = fromList;
        ImmutableList.copyOf(fromList).stream()
                .filter(toList::contains)
                .forEach(handler -> {
                    temp.remove(handler);
                    toList.remove(handler);
                });
        List<HandlerWrapper> finalList = new ArrayList<>();
        fromList.stream()
                .map(handler -> new HandlerWrapper(handler, Type.FROM))
                .forEach(finalList::add);
        toList.stream()
                .map(handler -> new HandlerWrapper(handler, Type.TO))
                .forEach(finalList::add);
        if (finalList.size() == 0) {
            this.last.put(player, new LastWrapper(toComplete, event.getToTransform().getPosition()));
            return;
        }
        Collections.sort(finalList);
        int currPriority = finalList.get(0).handler.getPriority();
        Tristate flagState = Tristate.UNDEFINED;
        for (HandlerWrapper wrap : finalList) {
            if (wrap.handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            if (wrap.type == Type.FROM) {
                flagState = flagState.and(wrap.handler.handle(player, Flag.PLAYER_EXIT, event).getState());
            } else {
                flagState = flagState.and(wrap.handler.handle(player, Flag.PLAYER_ENTER, event).getState());
            }
            currPriority = wrap.handler.getPriority();
        }
        flagState = Flag.PLAYER_PASS.resolve(flagState);

        if (flagState == Tristate.FALSE) {
            player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission to pass!"));
            Vector3d position = this.last.get(player).position;
            if (position == null) position = event.getFromTransform().getPosition();
            event.setToTransform(event.getToTransform().setPosition(position));
        } else {
            this.last.put(player, new LastWrapper(toComplete, event.getToTransform().getPosition()));
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }

    }

    private enum Type {
        FROM, TO
    }

    private class HandlerWrapper implements Comparable<HandlerWrapper> {
        public IHandler handler;
        public Type type;

        public HandlerWrapper(IHandler handler, Type type) {
            this.handler = handler;
            this.type = type;
        }

        @Override
        public int compareTo(HandlerWrapper w) {
            int val = handler.compareTo(w.handler);
            if (val != 0) return val;
            else return type.compareTo(w.type);
        }

        @Override
        public String toString() {
            return this.type + ":" + this.handler;
        }
    }

    private class LastWrapper {
        public List<IHandler> list;
        public Vector3d position;

        public LastWrapper(List<IHandler> list, Vector3d position) {
            this.list = list;
            this.position = position;
        }
    }

    public class Listeners {
        @Listener
        public void onJoin(ClientConnectionEvent.Join event) {
            last.put(event.getTargetEntity(), new LastWrapper(null, event.getTargetEntity().getTransform().getPosition()));
        }

        @Listener
        public void onChange(FGUpdateEvent event) {
            last.clear();
        }
    }
}
