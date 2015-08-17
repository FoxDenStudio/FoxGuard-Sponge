package tk.elektrofuchse.fox.foxguard.listener;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.EventHandler;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.block.BlockEvent;
import org.spongepowered.api.event.block.BlockPlaceEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

/**
 * Created by Fox on 8/16/2015.
 */
public class BlockEventHandler implements EventHandler<BlockEvent>{

    @Override
    public void handle(BlockEvent event) throws Exception {
        if(!event.getCause().isPresent() || !(event.getCause().get() instanceof Player)) return;

        ActiveFlags flag = null;
        Vector3i loc = event.getLocation().getBlockPosition();
        Extent extent = event.getLocation().getExtent();
        World world;
        Player player = (Player) event.getCause().get();
        if(extent instanceof Chunk) {
            world = ((Chunk) extent).getWorld();
        } else {
            world = (World)extent;
        }
        if(event instanceof BlockBreakEvent) flag = ActiveFlags.BLOCK_BREAK;
        if(event instanceof BlockPlaceEvent) flag = ActiveFlags.BLOCK_PLACE;
        for(IRegion region : FoxGuardManager.getInstance().regions.get(world)){
            if(region.isInRegion(loc)){
                for(IFlagSet flagSet : region.getFlagSets()){
                    flagSet.hasPermission(player, flag);
                }
            }
        }
    }
}
