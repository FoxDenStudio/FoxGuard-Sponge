package net.foxdenstudio.sponge.foxguard.plugin.controller.message;

import net.foxdenstudio.sponge.foxguard.plugin.listener.util.ISendableMessage;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;

/**
 * Created by Fox on 4/11/2016.
 */
public class TitleMessage implements ISendableMessage {

    private final Title title;

    public TitleMessage(Title title) {
        this.title = title;
    }

    @Override
    public void send(Player player) {
        player.sendTitle(title);
    }

    @Override
    public Text preview() {
        Text.Builder builder = Text.builder();
        if(title.getTitle().isPresent()){
            builder.append(title.getTitle().get());
            if(title.getSubtitle().isPresent()) builder.append(Text.of(TextColors.RESET, " - "));
        }
        if(title.getSubtitle().isPresent()) builder.append(title.getSubtitle().get());
        return builder.build();
    }
}
