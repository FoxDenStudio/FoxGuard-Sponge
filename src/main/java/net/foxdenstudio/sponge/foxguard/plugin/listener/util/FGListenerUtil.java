package net.foxdenstudio.sponge.foxguard.plugin.listener.util;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.*;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;

public class FGListenerUtil {

    public static void applyEntityFlags(Entity entity, boolean[] flags){
        if (entity instanceof Living) {
            flags[LIVING.id] = true;
            if (entity instanceof Agent) {
                flags[MOB.id] = true;
                if (entity instanceof Hostile) {
                    flags[HOSTILE.id] = true;
                } else if (entity instanceof Human) {
                    flags[HUMAN.id] = true;
                } else {
                    flags[PASSIVE.id] = true;
                }
            } else if (entity instanceof Player) {
                flags[PLAYER.id] = true;
            } else if (entity instanceof ArmorStand) {
                flags[ARMORSTAND.id] = true;
            }
        } else if (entity instanceof Hanging) {
            flags[HANGING.id] = true;
        } else if (entity instanceof Boat) {
            flags[BOAT.id] = true;
        } else if (entity instanceof Minecart) {
            flags[MINECART.id] = true;
        }
    }
}
