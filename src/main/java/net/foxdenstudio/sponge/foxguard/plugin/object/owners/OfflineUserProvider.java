package net.foxdenstudio.sponge.foxguard.plugin.object.owners;

import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.GuavaCollectors;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Created by Fox on 12/31/2016.
 */
public class OfflineUserProvider implements IDisplayableOwnerProvider {

    private static final String[] ALIASES = {"offline", "off", "o"};

    private final UserStorageService service = FoxGuardMain.instance().getUserStorage();

    @Override
    public List<String> getOwnerKeywords() {
        return service.getAll().stream()
                .map(GameProfile::getName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public Optional<UUID> getOwnerUUID(@Nullable String keyword) {
        if(keyword == null || keyword.isEmpty()) return Optional.empty();
        return service.get(keyword).map(User::getUniqueId);
    }

    @Override
    public Optional<String> getKeyword(UUID owner) {
        return service.get(owner).map(User::getName);
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }
}
