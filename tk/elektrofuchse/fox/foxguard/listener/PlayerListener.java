package tk.elektrofuchse.fox.foxguard.listener;

import org.spongepowered.api.event.Subscribe;
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
    public void onPlayerJoin(PlayerJoinEvent event){
        FoxGuardCommand.getInstance().getStateMap().put(event.getEntity(), new CommandState());
        FoxGuardMain.getInstance().getLogger().info("A PLAYER HAS JOINED!");
    }

    @Subscribe
    public void onPlayerLeave(PlayerQuitEvent event){
        FoxGuardCommand.getInstance().getStateMap().remove(event.getEntity());
        FoxGuardMain.getInstance().getLogger().info("A PLAYER HAS LEFT!");
    }
}
