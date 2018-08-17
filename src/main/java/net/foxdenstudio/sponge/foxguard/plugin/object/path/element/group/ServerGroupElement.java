package net.foxdenstudio.sponge.foxguard.plugin.object.path.element.group;

import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.PathManager;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathElement;
import net.foxdenstudio.sponge.foxguard.plugin.object.path.element.IPathOnlyElement;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.Identifiable;

import java.util.*;

public class ServerGroupElement extends ExtendedGroupElement {

    @Override
    public Optional<? extends IPathElement> getExtension(String name) {
        return PathManager.getInstance().getServerGroupExtension(name);
    }

    @Override
    public Map<String, ? extends IPathElement> getExtensions() {
        return PathManager.getInstance().getServerGroupExtensions();
    }

    public static class PlayerAccessNameElement implements IPathOnlyElement {

        @Override
        public Optional<? extends IPathElement> resolve(String name) {
            Optional<Player> playerOptional = Sponge.getServer().getPlayer(name);

            return playerOptional.map(player -> PathManager.getInstance().getUserLocalGroup(player.getUniqueId()));
        }

        @Override
        public Collection<String> getPathSuggestions() {
            return Sponge.getServer().getOnlinePlayers().stream()
                    .map(User::getName)
                    .sorted()
                    .collect(GuavaCollectors.toImmutableList());
        }
    }

    public static class PlayerAccessOfflineElement implements IPathOnlyElement {

        @Override
        public Optional<? extends IPathElement> resolve(String name) {
            UserStorageService service = FoxGuardMain.instance().getUserStorage();
            Optional<User> playerOptional = service.get(name);

            return playerOptional.map(player -> PathManager.getInstance().getUserLocalGroup(player.getUniqueId()));
        }

        @Override
        public Collection<String> getPathSuggestions() {
            UserStorageService service = FoxGuardMain.instance().getUserStorage();
            return service.getAll().stream()
                    .map(org.spongepowered.api.profile.GameProfile::getName)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted()
                    .collect(GuavaCollectors.toImmutableList());
        }
    }

    public static class PlayerAccessUUIDElement implements IPathOnlyElement {

        public static final String UUID_REGEX = "[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}";

        @Override
        public Optional<? extends IPathElement> resolve(String name) {
            UUID uuid = null;

            if (name.matches(UUID_REGEX)) {
                uuid = UUID.fromString(name);
            } else {
                name = name.replace("-", "");
                if (name.matches("[\\da-f]+")) {
                    if (name.length() == 32) {
                        name = name.substring(0, 8) + "-"
                                + name.substring(8, 12) + "-"
                                + name.substring(12, 16) + "-"
                                + name.substring(16, 20) + "-"
                                + name.substring(20, 32);
                        uuid = UUID.fromString(name);
                    } else if (name.length() < 32) {
                        List<UUID> candidates = getCandidates(name);
                        if (candidates.size() == 1) uuid = candidates.get(0);
                    }
                }
            }
            if (uuid == null) return Optional.empty();
            return Optional.ofNullable(PathManager.getInstance().getUserLocalGroup(uuid));
        }

        @Override
        public Collection<String> getPathSuggestions() {
            UserStorageService service = FoxGuardMain.instance().getUserStorage();

            return service.getAll().stream()
                    .map(Identifiable::getUniqueId)
                    .map(UUID::toString)
                    .sorted()
                    .collect(GuavaCollectors.toImmutableList());
        }

        private List<UUID> getCandidates(String name) {
            UserStorageService service = FoxGuardMain.instance().getUserStorage();
            name = name.replace("-", "");

            List<UUID> candidates = new ArrayList<>();
            for (GameProfile profile : service.getAll()) {
                UUID id = profile.getUniqueId();
                if (id.toString().replace("-", "").startsWith(name)) {
                    candidates.add(id);
                }
            }
            return candidates;
        }
    }
}
