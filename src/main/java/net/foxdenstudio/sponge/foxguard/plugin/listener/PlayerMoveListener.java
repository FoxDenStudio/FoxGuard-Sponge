package net.foxdenstudio.sponge.foxguard.plugin.listener;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by Fox on 1/4/2016.
 * Project: SpongeForge
 */
public class PlayerMoveListener implements EventListener<DisplaceEntityEvent.Move.TargetPlayer> {
    @Override
    public void handle(DisplaceEntityEvent.Move.TargetPlayer event) throws Exception {
        if (event.isCancelled()) return;
        Player player = event.getTargetEntity();
        World world = player.getWorld();
        Vector3d from = event.getFromTransform().getPosition(), to = event.getToTransform().getPosition();
        LinkedList<IHandler> fromList = new LinkedList<>(), toList = new LinkedList<>();

        FGManager.getInstance().getRegionsList(world, new Vector3i(
                GenericMath.floor(from.getX() / 16.0),
                GenericMath.floor(from.getY() / 16.0),
                GenericMath.floor(from.getZ() / 16.0))).stream()
                .filter(region -> region.isInRegion(from))
                .filter(IFGObject::isEnabled)
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !fromList.contains(handler))
                        .forEach(fromList::add));

        FGManager.getInstance().getRegionsList(world, new Vector3i(
                GenericMath.floor(to.getX() / 16.0),
                GenericMath.floor(to.getY() / 16.0),
                GenericMath.floor(to.getZ() / 16.0))).stream()
                .filter(region -> region.isInRegion(to))
                .filter(IFGObject::isEnabled)
                .forEach(region -> region.getHandlers().stream()
                        .filter(IFGObject::isEnabled)
                        .filter(handler -> !toList.contains(handler))
                        .forEach(toList::add));


        ImmutableList.copyOf(fromList).stream()
                .filter(toList::contains)
                .forEach(handler -> {
                    fromList.remove(handler);
                    toList.remove(handler);
                });

        Collections.sort(fromList);
        Collections.sort(toList);


    }
}
