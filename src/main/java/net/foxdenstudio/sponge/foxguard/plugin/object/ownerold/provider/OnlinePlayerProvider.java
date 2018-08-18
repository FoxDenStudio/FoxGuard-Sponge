package net.foxdenstudio.sponge.foxguard.plugin.object.ownerold.provider;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Identifiable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Fox on 12/31/2016.
 */
public class OnlinePlayerProvider implements IDisplayableOwnerProvider {

    private static final String[] ALIASES = {"player", "p"};

    @Override
    public List<String> getOwnerKeywords() {
        return Sponge.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public Optional<UUID> getOwnerUUID(String keyword) {
        if (keyword == null || keyword.isEmpty()) return Optional.empty();
        return Sponge.getServer().getPlayer(keyword).map(Player::getUniqueId);
    }


    @Override
    public Optional<String> getKeyword(UUID owner) {
        return Sponge.getServer().getPlayer(owner).map(Player::getName);
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    public Optional<Text> getDisplayText(UUID owner, @Nullable CommandSource viewer) {
        return getDisplayName(owner, viewer)
                .map(name->{
                    TextColor color = TextColors.GREEN;
                    if (viewer instanceof Identifiable && ((Identifiable) viewer).getUniqueId().equals(owner))
                        color = TextColors.AQUA;
                    return Text.of(color, name);
                });
    }
}
