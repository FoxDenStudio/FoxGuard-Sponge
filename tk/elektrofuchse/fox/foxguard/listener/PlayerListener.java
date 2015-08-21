package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChangeWorldEvent;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.commands.FoxGuardCommand;
import tk.elektrofuchse.fox.foxguard.commands.util.CommandState;

/**
 * Created by Fox on 8/20/2015.
 */
public class PlayerListener {

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        FoxGuardCommand.getInstance().getStateMap().put(event.getEntity(), new CommandState());
    }

    @Subscribe
    public void onPlayerLeave(PlayerQuitEvent event) {
        FoxGuardCommand.getInstance().getStateMap().remove(event.getEntity());
    }

    @Subscribe
    public void onPlayerEnterWorld(PlayerChangeWorldEvent event) {
        FoxGuardCommand.getInstance().getStateMap().get(event.getEntity()).flush();
    }
}
