/*
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 */

package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.living.player.TargetPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import tk.elektrofuchse.fox.foxguard.commands.FGCommandMainDispatcher;

/**
 * Created by Fox on 8/20/2015.
 * Project: foxguard
 */
public class PlayerEventListener implements EventListener<TargetPlayerEvent> {

    @Override
    public void handle(TargetPlayerEvent event) {
        if (event instanceof ClientConnectionEvent.Disconnect) {
            FGCommandMainDispatcher.getInstance().getStateMap().remove(event.getTargetEntity());

        }
    }
}
