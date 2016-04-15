package net.foxdenstudio.sponge.foxguard.plugin.controller.message;

import net.foxdenstudio.sponge.foxguard.plugin.listener.util.ISendableMessage;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatType;

/**
 * Created by Fox on 4/10/2016.
 */
public final class ChatMessage implements ISendableMessage {
    public final Text text;
    public final ChatType type;

    public ChatMessage(Text text, ChatType type) {
        this.text = text;
        this.type = type;
    }

    @Override
    public void send(Player player) {
        player.sendMessage(type, text);
    }

    @Override
    public Text preview() {
        return Text.builder().append(Text.of(type.toString() + ": ")).append(text).build();
    }
}
