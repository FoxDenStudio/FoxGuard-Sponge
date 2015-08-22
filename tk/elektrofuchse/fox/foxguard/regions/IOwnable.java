package tk.elektrofuchse.fox.foxguard.regions;

import org.spongepowered.api.entity.player.Player;

import java.util.List;

/**
 * Created by Fox on 8/22/2015.
 */
public interface IOwnable {

    List<Player> getOwners();

    void setOwners(List<Player> owners);

    boolean addOwner(Player player);

    boolean removeOwner(Player player);
}
