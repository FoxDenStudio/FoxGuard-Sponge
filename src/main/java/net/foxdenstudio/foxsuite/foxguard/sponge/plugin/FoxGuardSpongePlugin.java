package net.foxdenstudio.foxsuite.foxguard.sponge.plugin;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "foxguard",
        name = "FoxGuard",
        dependencies = {
                @Dependency(id = "foxcore")
        },
        description = "A world protection plugin built for SpongeAPI. Requires FoxCore.",
        authors = {"gravityfox", "d4rkfly3r", "vectrix", "Waterpicker"},
        url = "https://github.com/FoxDenStudio/FoxGuard")
public class FoxGuardSpongePlugin {


}
