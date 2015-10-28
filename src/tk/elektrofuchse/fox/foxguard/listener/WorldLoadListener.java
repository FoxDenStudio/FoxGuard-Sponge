package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import tk.elektrofuchse.fox.foxguard.FoxGuardStorageManager;

/**
 * Created by Fox on 10/28/2015.
 */
public class WorldLoadListener implements EventListener<LoadWorldEvent> {
    @Override
    public void handle(LoadWorldEvent event) throws Exception {
        FoxGuardStorageManager.getInstance().initWorld(event.getTargetWorld());
    }
}
