package net.foxdenstudio.sponge.foxguard.plugin.listener.util;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

/**
 * Created by Fox on 4/10/2016.
 */
public interface ISendableMessage {

    void send(Player player);

    Text preview();



}
