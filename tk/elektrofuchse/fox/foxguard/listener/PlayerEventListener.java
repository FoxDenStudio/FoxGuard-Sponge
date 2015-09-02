package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.entity.living.player.PlayerEvent;
import org.spongepowered.api.event.entity.living.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.living.player.PlayerQuitEvent;
import tk.elektrofuchse.fox.foxguard.commands.FoxGuardCommand;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandState;

/**
 * Created by Fox on 8/20/2015.
 */
public class PlayerEventListener implements EventListener<PlayerEvent> {

    @Override
    public void handle(PlayerEvent event) {
        if(event instanceof PlayerJoinEvent){
            FoxGuardCommand.getInstance().getStateMap().put(event.getSourceEntity(), new CommandState());
        } else if (event instanceof PlayerQuitEvent){
            FoxGuardCommand.getInstance().getStateMap().remove(event.getSourceEntity());
        }
    }
}
