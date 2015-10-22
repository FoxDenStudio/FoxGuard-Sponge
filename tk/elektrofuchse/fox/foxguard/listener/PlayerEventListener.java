package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.living.player.TargetPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import tk.elektrofuchse.fox.foxguard.commands.FoxGuardCommand;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

/**
 * Created by Fox on 8/20/2015.
 */
public class PlayerEventListener implements EventListener<TargetPlayerEvent> {

    @Override
    public void handle(TargetPlayerEvent event) {
        if (event instanceof ClientConnectionEvent.Join) {
            FoxGuardCommand.getInstance().getStateMap().put(event.getTargetEntity(), new InternalCommandState());
        } else if (event instanceof ClientConnectionEvent.Disconnect) {
            FoxGuardCommand.getInstance().getStateMap().remove(event.getTargetEntity());
        }
    }
}
