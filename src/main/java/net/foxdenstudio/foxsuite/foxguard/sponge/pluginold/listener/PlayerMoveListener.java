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

package net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.listener;

import com.flowpowered.math.vector.Vector3d;
import net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag.FlagSet;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.util.*;

import static net.foxdenstudio.foxsuite.foxguard.sponge.pluginold.flag.Flags.*;

/**
 * Created by Fox on 1/4/2016.
 * Project: SpongeForge
 */
public class PlayerMoveListener implements EventListener<MoveEntityEvent> {

    private static final boolean[] BASE_FLAG_SET = FlagSet.arrayFromFlags(ROOT, DEBUFF, MOVE);

    private static PlayerMoveListener instance;

    public final boolean full;

    private final Map<Entity, Boolean> lastRiding = new WeakHashMap<>();
    private final Map<Player, Vector3d> lastValidPosition = new WeakHashMap<>();

    /*private final Map<Player, Scoreboard> scoreboardMap = new WeakCacheMap<>((k, m) -> {
        if (k instanceof Player) {
            Scoreboard s = Scoreboard.builder().build();
            Objective o = Objective.builder().criterion(Criteria.DUMMY).name("foxguardhere").displayName(Text.EMPTY).build();
            s.addObjective(o);
            s.updateDisplaySlot(o, DisplaySlots.SIDEBAR);
            m.put((Player) k, s);
            return s;
        } else return null;
    });
    private final Map<Player, HUDConfig> hudConfigMap = new WeakCacheMap<>((k, m) -> new HUDConfig());*/

    public PlayerMoveListener(boolean full) {
        this.full = full;
        if (instance == null) instance = this;
    }

    @Override
    public void handle(@Nonnull MoveEntityEvent event) {
        Entity entity = event.getTargetEntity();
        boolean isRiding = entity.getVehicle().isPresent();
        boolean wasRiding = lastRiding.getOrDefault(entity, isRiding);
        lastRiding.put(entity, isRiding);

        Set<Player> passengerStack = getPassengerStack(event.getTargetEntity());
//        if (!passengerStack.isEmpty()) {
//            System.out.println("Entity: " + entity);
//            System.out.println("Passengers: " + passengerStack);
//            System.out.println(event.isCancelled());
//            System.out.println(isRiding);
//            System.out.println(wasRiding);
//        }

//        if(entity instanceof Player){
//            Player player = ((Player) entity);
//            Optional<Entity> vehicleOpt = player.getVehicle();
//            if(vehicleOpt.isPresent()){
//                Entity vehicle = vehicleOpt.get();
//                if(vehicle instanceof Horse){
//                    event.setCancelled(true);
//                    return;
//                }
//            }
//        }

        if (event.isCancelled()) return;

        if (isRiding && wasRiding) return;


        if (!passengerStack.isEmpty()) {

//            FGManager manager = FGManager.getInstance();

            Transform<World> from = event.getFromTransform();
            Transform<World> to = event.getToTransform();

            /*Set<IRegion> initialRegions, finalRegions = new HashSet<>();
            Set<IHandler> finalHandlers;

            initialRegions = manager.getRegionsAtPos(from.getExtent(), from.getPosition());*/

            boolean noToRegions = true;
            int matchedRegionsCount = 0;
            /*for (IRegion region : manager.getRegionsInChunkAtPos(to.getExtent(), to.getPosition())) {
                if (region.contains(to.getPosition(), to.getExtent())) {
                    finalRegions.add(region);
                    if (noToRegions) {
                        if (initialRegions.contains(region)) {
                            ++matchedRegionsCount;
                        } else {
                            noToRegions = false;
                        }
                    }
                }
            }

            // Check change in regions
            if (noToRegions && initialRegions.size() == matchedRegionsCount) return;

            finalHandlers = finalRegions.stream()
                    .flatMap(region -> region.getHandlers().stream())
                    .collect(Collectors.toSet());

            for (Player player : passengerStack) {
                final boolean hud = player.getScoreboard() == scoreboardMap.get(player) && CommandHUD.instance().getIsHUDEnabled().get(player);
                if (hud) {
                    final HUDConfig config = this.hudConfigMap.get(player);
                    renderHUD(player, finalRegions, finalHandlers, config);
                    player.setScoreboard(this.scoreboardMap.get(player));
                }
            }*/

            if (full) {
                /*Set<IHandler> fromHandlers, toHandlers = new HashSet<>();

                fromHandlers = initialRegions.stream()
                        .flatMap(region -> region.getHandlers().stream())
                        .collect(Collectors.toSet());
                finalHandlers.forEach(handler -> {
                    if (!fromHandlers.remove(handler)) toHandlers.add(handler);
                });

                // check change in handlers
                if (fromHandlers.isEmpty() && toHandlers.isEmpty()) return;

                List<HandlerWrapper> handlersList = new ArrayList<>();

                for (IHandler handler : fromHandlers) {
                    if (handler.isEnabled())
                        handlersList.add(new HandlerWrapper(handler, Type.FROM));
                }

                for (IHandler handler : toHandlers) {
                    if (handler.isEnabled())
                        handlersList.add(new HandlerWrapper(handler, Type.TO));
                }

                Collections.sort(handlersList);

                int currPriority = handlersList.get(0).handler.getPriority();*/
                Tristate flagState = Tristate.UNDEFINED;

                boolean[] exitFlags = BASE_FLAG_SET.clone();
                if (event instanceof MoveEntityEvent.Teleport) {
                    exitFlags[TELEPORT.id] = true;
                    if (event instanceof MoveEntityEvent.Teleport.Portal) {
                        exitFlags[PORTAL.id] = true;
                    }
                }
                boolean[] enterFlags = exitFlags.clone();
                exitFlags[EXIT.id] = true;
                enterFlags[ENTER.id] = true;

                FlagSet exitFlagSet = new FlagSet(exitFlags);
                FlagSet enterFlagSet = new FlagSet(enterFlags);

                Player offendingPlayer = null;
                for (Player player : passengerStack) {
                    /*for (HandlerWrapper wrap : handlersList) {
                        if (wrap.handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                            break;
                        }
                        EventResult result;
                        if (wrap.type == Type.FROM) {
                            result = wrap.handler.handle(player, exitFlagSet, ExtraContext.of(event));
                        } else
                            result = wrap.handler.handle(player, enterFlagSet, ExtraContext.of(event));
                        flagState = flagState.and(result.getState());
                        currPriority = wrap.handler.getPriority();
                    }*/
                    if (flagState == Tristate.FALSE) {
                        offendingPlayer = player;
                        break;
                    }
                }

                if (flagState == Tristate.FALSE) {
                    for (Player player : passengerStack) {
                        if (player == offendingPlayer) {
                            player.sendMessage(ChatTypes.ACTION_BAR, Text.of("You don't have permission to pass!"));
                        } else {
                            player.sendMessage(ChatTypes.ACTION_BAR, Text.of("Someone else doesn't have permission to pass!"));
                        }
                    }
                    event.setCancelled(true);
//                    Vector3d position = this.last.get(player).position;
//                    if (position == null) position = event.getFromTransform().getPosition();
//                    event.setToTransform(event.getToTransform().setPosition(position));
                } else {
//                    this.last.put(player, new PlayerMoveListener.LastWrapper(toComplete, event.getToTransform().getPosition()));

                    // makes sure that handlers are unable to cancel the event directly.
                    event.setCancelled(false);
                }
            }
        }

        lastRiding.put(entity, isRiding);
    }

    private enum Type {
        FROM, TO
    }

    /*private class HandlerWrapper implements Comparable<HandlerWrapper> {
        public IHandler handler;
        public Type type;

        public HandlerWrapper(@Nonnull IHandler handler, @Nonnull Type type) {
            this.handler = handler;
            this.type = type;
        }

        @Override
        public int compareTo(HandlerWrapper w) {
            int val = handler.compareTo(w.handler);
            return val != 0 ? val : type.compareTo(w.type);
        }

        @Override
        public String toString() {
            return this.type + ":" + this.handler;
        }
    }*/

    /*@SuppressWarnings("Duplicates")
    public void renderHUD(Player player, Collection<IRegion> regions, Collection<IHandler> handlers, HUDConfig config) {
        this.scoreboardMap.remove(player);
        Scoreboard scoreboard = this.scoreboardMap.get(player);
        Objective objective = scoreboard.getObjective("foxguardhere").get();
        if (config.regions) {
            List<IRegion> regionList = new ArrayList<>(regions);
            Collections.sort(regionList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (config.handlers) {
                List<IHandler> handlerList = new ArrayList<>(handlers);
                if (config.priority) {
                    // TODO redo sorting post owners
                    Collections.sort(handlerList, (o1, o2) -> o2.getPriority() - o1.getPriority());
                } else {
                    Collections.sort(handlerList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                }
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Regions and Handlers Here  "));
                final int total = regionList.size() + handlerList.size();
                final int regionCount = (int) Math.round(13.0 * regionList.size() / total);
                final int handlerCount = (int) Math.round(13.0 * handlerList.size() / total);
                int slot = Math.min(15, total + 2);
                Score regionsScore = objective.getOrCreateScore(Text.of(TextColors.GREEN, "Regions (" + player.getWorld().getName() + ") ",
                        TextColors.YELLOW, "(" + regionList.size() + ")"));
                regionsScore.setScore(slot--);
                for (int i = 0; i < regionCount && i < regionList.size(); i++) {
                    IRegion region = regionList.get(i);
                    // TODO redo naming post owners. Actually this whole thing will need some tweaking.
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(region),
                            "  " + FGUtil.getRegionName(region, false)));
                    score.setScore(slot--);
                }
                Score handlersScore = objective.getOrCreateScore(Text.of(TextColors.GREEN, "Handlers " + (config.priority ? "by Priority " : ""),
                        TextColors.YELLOW, "(" + handlerList.size() + ")"));
                handlersScore.setScore(slot--);
                for (int i = 0; i < handlerCount && i < handlerList.size(); i++) {
                    IHandler handler = handlerList.get(i);
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(handler),
                            "  " + handler.getShortTypeName() + " : " + handler.getName()));
                    score.setScore(slot--);
                }

            } else {
                int slot = regionList.size();
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Regions Here (" + player.getWorld().getName() + ")  "));
                for (IRegion region : regionList) {
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(region),
                            "  " + FGUtil.getRegionName(region, false)));
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
                List<IHandler> handlerList = new ArrayList<>(handlers);
                int slot = handlerList.size();
                objective.setDisplayName(Text.of(TextColors.GOLD, "  Handlers Here  "));
                Collections.sort(handlerList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                for (IHandler handler : handlerList) {
                    Score score = objective.getOrCreateScore(Text.of(FGUtil.getColorForObject(handler),
                            "  " + handler.getShortTypeName() + " : " + handler.getName()));
                    score.setScore(slot--);
                    if (slot <= 0) break;
                }
            }
        }
    }*/

    /*public Map<Player, HUDConfig> getHudConfigMap() {
        return hudConfigMap;
    }*/
    /*public void showScoreboard(Player player) {
        player.setScoreboard(this.scoreboardMap.get(player));
    }*/

    public static PlayerMoveListener getInstance() {
        return instance;
    }

    private static Set<Player> getPassengerStack(Entity e) {
        Set<Player> set = new HashSet<>();
        if (e instanceof Player) set.add((Player) e);
        List<Entity> po = e.getPassengers();
        for (Entity ent : po) {
            set.addAll(getPassengerStack(ent));
        }
        return set;
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
}
