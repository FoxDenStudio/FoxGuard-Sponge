package tk.elektrofuchse.fox.foxguard.pieces;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.ban.Ban;

import java.util.List;
import java.util.UUID;

/**
 * Created by Fox on 8/22/2015.
 * Project: foxguard
 */
public interface IOwnable {

    List<User> getOwners();

    void setOwners(List<User> owners);

    boolean addOwner(User player);

    boolean removeOwner(User player);
}
