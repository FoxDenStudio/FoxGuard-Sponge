package net.foxdenstudio.sponge.foxguard.plugin.config;

import net.foxdenstudio.sponge.foxguard.plugin.FoxGuardMain;
import net.foxdenstudio.sponge.foxguard.plugin.listener.PlayerMoveListenerNew;
import org.spongepowered.api.event.entity.MoveEntityEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum ListenerModule {

    MOVEMENT("movement", "true", "Sets movement on or off. \"hud\" enables listener, but only for /fg here functionality\n" +
            " Options: true, false, hud") {
        @Override
        public String setup(String setting) {
            FoxGuardMain plugin = FoxGuardMain.instance();
            boolean full;
            if (setting.equalsIgnoreCase("false")) {
                return setting;
            } else if (setting.equalsIgnoreCase("true")) {
                full = true;
            } else if (setting.equalsIgnoreCase("hud")) {
                full = false;
            } else {
                setting = getDefaultValue();
                full = true;
            }
            PlayerMoveListenerNew pml = new PlayerMoveListenerNew(true);
            plugin.registerListener(MoveEntityEvent.class, pml);
            //plugin.registerListeners(pml.new Listeners());
            return setting;
        }
    },;

    private final String name;
    private final String defaultValue;
    private final String comment;

    ListenerModule(@Nonnull String name, @Nonnull String defaultValue, @Nullable String comment) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.comment = comment != null && comment.isEmpty() ? null : comment;
    }

    public final String getName() {
        return name;
    }

    public String setup(String setting) {
        return setting;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Nullable
    public String getComment() {
        return comment;
    }
}
