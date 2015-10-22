package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;

/**
 * Created by Fox on 10/21/2015.
 */
public class RightClickHandler implements EventListener<InteractBlockEvent> {

    @Override
    public void handle(InteractBlockEvent event) throws Exception {
        FoxGuardMain.getInstance().getLogger().info(event.getTargetBlock().toString());
    }
}
