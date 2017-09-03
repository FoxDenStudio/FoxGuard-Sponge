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
import net.foxdenstudio.sponge.foxcore.common.util.WeakCacheMap;
import net.foxdenstudio.sponge.foxcore.plugin.command.CommandHUD;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.flag.FlagBitSet;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.util.FGUtil;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.stream.Collectors;

import static net.foxdenstudio.sponge.foxguard.plugin.flag.Flags.*;

/**
 * Created by Fox on 1/4/2016.
 * Project: SpongeForge
 */
public class PlayerMoveListenerNew implements EventListener<MoveEntityEvent> {

    private static final FlagBitSet BASE_FLAG_SET = new FlagBitSet(ROOT, DEBUFF, MOVE);

    private static PlayerMoveListenerNew instance;

    public final boolean full;

    private final Map<Entity, Boolean> lastRiding = new WeakHashMap<>();
    private final Map<Player, Vector3d> lastValidPosition = new WeakHashMap<>();

    private final Map<Player, Scoreboard> scoreboardMap = new WeakCacheMap<>((k, m) -> {
        if (k instanceof Player) {
            Scoreboard s = Scoreboard.builder().build();
            Objective o = Objective.builder().criterion(Criteria.DUMMY).name("foxguardhere").displayName(Text.EMPTY).build();
            s.addObjective(o);
            s.updateDisplaySlot(o, DisplaySlots.SIDEBAR);
            m.put((Player) k, s);
            return s;
        } else return null;
    });
    private final Map<Player, HUDConfig> hudConfigMap = new WeakCacheMap<>((k, m) -> new HUDConfig());

    public PlayerMoveListenerNew(boolean full) {
        this.full = full;
        if (instance == null) instance = this;
    }

    public static PlayerMoveListenerNew getInstance() {
        return instance;
    }

    private static Set<Player> getPassengerStack(Entity e) {
        Set<Player> set = new HashSet<>();
        if (e instanceof Player) set.add((Player) e);
        List<Entity> po = e.getPassengers();
        if (!po.isEmpty()) {
            for (Entity ent : po) {
                set.addAll(getPassengerStack(ent));
            }
        }
        return set;
    }

    @Override
    public void handle(MoveEntityEvent event) throws Exception {
        Entity entity = event.getTargetEntity();
        boolean isRiding = entity.getVehicle().isPresent();
        if (!lastRiding.containsKey(entity)) lastRiding.put(entity, isRiding);
        if (event.isCancelled()) return;
        boolean wasRiding = lastRiding.get(entity);

        FlagBitSet enter = BASE_FLAG_SET.clone();

        if (isRiding && !wasRiding) {

        } else {
            Set<Player> passengerStack = getPassengerStack(event.getTargetEntity());

            if (!passengerStack.isEmpty()) {

                FGManager manager = FGManager.getInstance();
                World world = event.getTargetEntity().getWorld();

                Vector3d from = event.getFromTransform().getPosition();
                Vector3d to = event.getToTransform().getPosition();

                Set<IRegion> fromRegions, toRegions = new HashSet<>(), finalRegions = new HashSet<>();
                Set<IHandler> fromHandlers, toHandlers = new HashSet<>(), finalHandlers;

                fromRegions = manager.getRegionsAtPos(world, from);
                fromHandlers = fromRegions.stream().flatMap(region -> region.getLinks().stream()).collect(Collectors.toSet());

                manager.getRegionsInChunkAtPos(world, to).stream()
                        .filter(region -> region.contains(to, world))
                        .forEach(region -> {
                            finalRegions.add(region);
                            if (!fromRegions.remove(region)) toRegions.add(region);
                        });
                finalHandlers = finalRegions.stream().flatMap(region -> region.getLinks().stream()).collect(Collectors.toSet());
                finalHandlers.forEach(handler -> {
                    if (!fromHandlers.remove(handler)) toHandlers.add(handler);
                });

                passengerStack.forEach(player -> {
                    final boolean hud = player.getScoreboard() == scoreboardMap.get(player) && CommandHUD.instance().getIsHUDEnabled().get(player);
                    final HUDConfig config = this.hudConfigMap.get(player);
                    final boolean regionHUD = hud && config.regions;


                });
            }
        }

        lastRiding.put(entity, isRiding);
    }

    public void renderHUD(Player player, List<IRegion> regions, List<IHandler> handlers, HUDConfig config) {
        this.scoreboardMap.remove(player);
        Scoreboard scoreboard = this.scoreboardMap.get(player);
        Objective objective = scoreboard.getObjective("foxguardhere").get();
        if (config.regions) {
            Collections.sort(regions, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (config.handlers) {
                if (config.priority) {
                    Collections.sort(handlers, (o1, o2) -> o2.getPriority() - o1.getPriority());
                } else {
                    Collections.sort(handlers, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                }
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Regions and Handlers Here  "));
                final int total = regions.size() + handlers.size();
                final int regionCount = (int) Math.round(13.0 * regions.size() / total);
                final int handlerCount = (int) Math.round(13.0 * handlers.size() / total);
                int slot = Math.min(15, total + 2);
                Score regionsScore = objective.getOrCreateScore(Text.of(TextColors.GREEN, "Regions (" + player.getWorld().getName() + ") ",
                        TextColors.YELLOW, "(" + regions.size() + ")"));
                regionsScore.setScore(slot--);
                for (int i = 0; i < regionCount && i < regions.size(); i++) {
                    IRegion region = regions.get(i);
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(region),
                            "  " + FGUtil.getRegionDisplayName(region, false)));
                    score.setScore(slot--);
                }
                Score handlersScore = objective.getOrCreateScore(Text.of(TextColors.GREEN, "Handlers " + (config.priority ? "by Priority " : ""),
                        TextColors.YELLOW, "(" + handlers.size() + ")"));
                handlersScore.setScore(slot--);
                for (int i = 0; i < handlerCount && i < handlers.size(); i++) {
                    IHandler handler = handlers.get(i);
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(handler),
                            "  " + handler.getShortTypeName() + " : " + handler.getName()));
                    score.setScore(slot--);
                }

            } else {
                int slot = regions.size();
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Regions Here (" + player.getWorld().getName() + ")  "));
                for (IRegion region : regions) {
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(region),
                            "  " + FGUtil.getRegionDisplayName(region, false)));
                    score.setScore(slot--);
                    if (slot <= 0) break;
                }
            }
        } else if (config.handlers) {
            if (config.priority) {
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Handlers Here by Priority  "));
                for (IHandler handler : handlers) {
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(handler),
                            "  " + handler.getShortTypeName() + " : " + handler.getName()));
                    score.setScore(handler.getPriority());
                }
            } else {
                int slot = handlers.size();
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Handlers Here  "));
                Collections.sort(handlers, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                for (IHandler handler : handlers) {
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(handler),
                            "  " + handler.getShortTypeName() + " : " + handler.getName()));
                    score.setScore(slot--);
                    if (slot <= 0) break;
                }
            }
        }
    }

    public Map<Player, HUDConfig> getHudConfigMap() {
        return hudConfigMap;
    }

    public void showScoreboard(Player player) {
        player.setScoreboard(this.scoreboardMap.get(player));
    }

    private enum Direction {
        FROM, TO
    }

    public static class HUDConfig {
        public boolean regions;
        public boolean handlers;
        public boolean priority;

        public HUDConfig() {
            this(true, true, false);
        }

        public HUDConfig(boolean regions, boolean handlers, boolean priority) {
            this.regions = regions;
            this.handlers = handlers;
            this.priority = priority;
        }
    }

    private class HandlerWrapper implements Comparable<HandlerWrapper> {
        public IHandler handler;
        public Direction direction;

        public HandlerWrapper(IHandler handler, Direction direction) {
            this.handler = handler;
            this.direction = direction;
        }

        @Override
        public int compareTo(HandlerWrapper w) {
            int val = handler.compareTo(w.handler);
            return val != 0 ? val : direction.compareTo(w.direction);
        }

        @Override
        public String toString() {
            return this.direction + ":" + this.handler;
        }
    }
}
