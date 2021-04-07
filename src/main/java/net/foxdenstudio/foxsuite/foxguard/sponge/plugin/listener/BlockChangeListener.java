package net.foxdenstudio.foxsuite.foxguard.sponge.plugin.listener;

import net.foxdenstudio.foxsuite.foxcore.api.annotation.guice.FoxLogger;
import net.foxdenstudio.foxsuite.foxcore.api.object.reference.types.IndexReference;
import net.foxdenstudio.foxsuite.foxcore.api.path.FoxPath;
import net.foxdenstudio.foxsuite.foxcore.api.region.FoxRegion;
import net.foxdenstudio.foxsuite.foxcore.api.region.cache.RegionCache;
import net.foxdenstudio.foxsuite.foxguard.api.flag.EventData;
import net.foxdenstudio.foxsuite.foxguard.api.flag.FlagManager;
import net.foxdenstudio.foxsuite.foxguard.api.flag.FlagSet;
import net.foxdenstudio.foxsuite.foxguard.api.flag.FoxGuardCommonFlags;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BlockChangeListener implements EventListener<ChangeBlockEvent> {

    private final RegionCache regionCache;
    private final FoxGuardCommonFlags flags;

    private final FlagSet rootFlagSet;

    @FoxLogger("guard.listener.block")
    private Logger logger;

    @Inject
    public BlockChangeListener(RegionCache regionCache, FoxGuardCommonFlags flags) {
        this.regionCache = regionCache;
        this.flags = flags;
        this.rootFlagSet = flags.rootSchema.genFlagSet(flags.root, flags.debuff, flags.block, flags.change);
    }

    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        // Exclusions
        if (event.isCancelled() || event instanceof ExplosionEvent || event.getTransactions().isEmpty()) return;

        Set<FoxRegion> regionSet = new HashSet<>();
        for (Transaction<BlockSnapshot> tr : event.getTransactions()) {
            // Grass-dirt exclusion because sheep eat grass and grass spreads
            // These events are so insignificant and so frequent that it's not worth paying the performance hit
            // and nobody seems to notice that it slips past protections.
            BlockSnapshot oldBlock = tr.getOriginal();
            BlockType oldType = oldBlock.getState().getType();
            BlockType newType = tr.getFinal().getState().getType();
            if (oldType.equals(BlockTypes.DIRT) && newType.equals(BlockTypes.GRASS)
                    || oldType.equals(BlockTypes.GRASS) && newType.equals(BlockTypes.DIRT)) return;

            Location<World> loc = oldBlock.getLocation().get();
            regionSet.addAll(this.regionCache.getRegionsBlock(loc.getBlockPosition(), (net.foxdenstudio.foxsuite.foxcore.platform.world.World) loc.getExtent()));

            if (!regionSet.isEmpty()) {
                logger.info("Block event of size {} occurred in the following regions: {}", event.getTransactions().size(), printRegions(regionSet));
            }

            EventData eventData = new EventData(event, rootFlagSet);
        }

    }

    private static String printRegions(Collection<FoxRegion> collection) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<FoxRegion> it = collection.iterator(); it.hasNext(); ) {
            FoxRegion region = it.next();
            sb.append(region.getIndexReference().flatMap(IndexReference::getPrimePath).map(FoxPath::toString).orElse("<unknown>"));
            if (it.hasNext()) sb.append(", ");
        }
        return sb.toString();
    }

}
