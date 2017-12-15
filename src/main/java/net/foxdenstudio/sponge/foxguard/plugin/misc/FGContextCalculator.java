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

package net.foxdenstudio.sponge.foxguard.plugin.misc;

import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.IGlobal;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.World;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Fox on 10/16/2016.
 */
public class FGContextCalculator implements ContextCalculator<Subject> {

    public static final String foxguardRegionKey = "foxguardrg";

    @Override
    public void accumulateContexts(Subject calculable, Set<Context> accumulator) {
        if (calculable instanceof Player) {
            Player player = (Player) calculable;
            Set<IRegion> regions = FGManager.getInstance().getRegionsAtPos(player.getWorld(), player.getLocation().getPosition());
            StringBuilder builder = new StringBuilder();
            for (Iterator<IRegion> iterator = regions.iterator(); iterator.hasNext(); ) {
                IRegion region = iterator.next();
                if (region instanceof IGlobal || !region.getOwner().equals(FGManager.SERVER_UUID)) continue;

                if (region instanceof IWorldRegion) {
                    builder.append(((IWorldRegion) region).getWorld().getName()).append(":");
                }
                builder.append(region.getName().toLowerCase());
                if (iterator.hasNext()) builder.append(",");
            }
            accumulator.add(new Context(foxguardRegionKey, builder.toString()));
        }
    }

    @Override
    public boolean matches(Context context, Subject subject) {
        if (!context.getType().equals(foxguardRegionKey)) return false;
        if (subject instanceof Player) {
            Player player = (Player) subject;
            String[] regionNames = context.getValue().split(",");
            FGManager fgManager = FGManager.getInstance();
            for (String regionName : regionNames) {
                if (regionName.contains(":")) {
                    String[] parts = regionName.split(":");
                    Optional<World> worldOptional = Sponge.getServer().getWorld(parts[0]);
                    if (worldOptional.isPresent()) {
                        Optional<IWorldRegion> regionOpt = fgManager.getWorldRegion(worldOptional.get(), parts[1]);
                        if (regionOpt.isPresent()) {
                            if (!regionOpt.get().contains(player.getLocation().getPosition())) return false;
                        } else return false;
                    } else return false;
                } else {
                    Optional<IRegion> regionOpt = fgManager.getRegion(regionName);
                    if (regionOpt.isPresent()) {
                        if (!regionOpt.get().contains(player.getLocation().getPosition(), player.getWorld()))
                            return false;
                    } else return false;
                }
            }
            return true;
        } else return false;
    }
}
