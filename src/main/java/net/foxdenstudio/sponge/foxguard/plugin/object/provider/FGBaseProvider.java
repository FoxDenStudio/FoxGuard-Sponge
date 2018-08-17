package net.foxdenstudio.sponge.foxguard.plugin.object.provider;

import com.google.common.collect.ImmutableList;
import net.foxdenstudio.sponge.foxguard.plugin.controller.IController;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.IFGObject;
import net.foxdenstudio.sponge.foxguard.plugin.region.IRegion;
import net.foxdenstudio.sponge.foxguard.plugin.region.world.IWorldRegion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.util.GuavaCollectors;
import org.spongepowered.api.util.StartsWithPredicate;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Stream;

public class FGBaseProvider implements IObjectProvider {

    public static final String[] PREFIXES = {"r", "wr", "sr", "h", "c"};


    @Override
    public Collection<IFGObject> filter(@Nonnull Collection<IFGObject> objects, @Nonnull String prefix, @Nonnull String[] args, @Nonnull CommandSource source) {
        Stream<IFGObject> stream = objects.stream();

        switch (prefix) {
            case "r":
                stream = stream.filter((ifgObject -> {
                    if (ifgObject instanceof IRegion) {
                        if (ifgObject instanceof IWorldRegion && args.length > 0 && !args[0].isEmpty()) {
                            return ((IWorldRegion) ifgObject).getWorld().getName().equalsIgnoreCase(args[0]);
                        }
                        return true;
                    } else return false;
                }));
                break;
            case "wr":
                stream = stream.filter((ifgObject -> {
                    if (ifgObject instanceof IWorldRegion) {
                        if (args.length > 0 && !args[0].isEmpty()) {
                            return ((IWorldRegion) ifgObject).getWorld().getName().equalsIgnoreCase(args[0]);
                        }
                        return true;
                    } else return false;
                }));
                break;
            case "sr":
                stream = stream.filter((ifgObject -> {
                    if (ifgObject instanceof IRegion) {
                        return !(ifgObject instanceof IWorldRegion);
                    } else return false;
                }));
                break;
            case "h":
                stream = stream.filter((ifgObject -> ifgObject instanceof IHandler));
                break;
            case "c":
                stream = stream.filter((ifgObject -> ifgObject instanceof IController));
                break;
        }

        return stream.collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public Collection<String> getFilterSuggestions(@Nonnull String prefix, @Nonnull String[] args, @Nonnull CommandSource source) {
        if (args.length == 1 && (prefix.equalsIgnoreCase("r") || prefix.equalsIgnoreCase("wr"))) {
            return Sponge.getServer().getWorlds().stream()
                    .map(World::getName)
                    .filter(new StartsWithPredicate(args[0]))
                    .collect(GuavaCollectors.toImmutableList());
        } else return ImmutableList.of();
    }

    @Override
    public String[] getPrefixes() {
        return PREFIXES;
    }
}
