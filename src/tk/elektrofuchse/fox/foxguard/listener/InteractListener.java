package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.commands.util.InternalCommandState;

/**
 * Created by Fox on 10/21/2015.
 * Project: foxguard
 */
public class InteractListener implements EventListener<InteractEvent> {

    @Override
    public void handle(InteractEvent event) throws Exception {
        if (event instanceof InteractEntityEvent){

        }
    }
}
