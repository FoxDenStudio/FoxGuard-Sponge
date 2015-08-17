package tk.elektrofuchse.fox.foxguard.listener;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.EventHandler;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.block.BlockChangeEvent;
import org.spongepowered.api.event.block.BlockEvent;
import org.spongepowered.api.event.block.BlockPlaceEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.regions.IRegion;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 */
public class BlockEventHandler implements EventHandler<BlockChangeEvent>{

    @Override
    public void handle(BlockChangeEvent event) throws Exception {
        if(!event.getCause().isPresent() || !(event.getCause().get().getCause() instanceof Player)) return;
        ActiveFlags typeFlag = null;
        if (event instanceof BlockBreakEvent) typeFlag = ActiveFlags.BLOCK_BREAK;
        else if (event instanceof BlockPlaceEvent) typeFlag = ActiveFlags.BLOCK_PLACE;
        else return;
        Player player = (Player) event.getCause().get().getCause();
        Vector3i loc = event.getLocation().getBlockPosition();
        Extent extent = event.getLocation().getExtent();
        World world;

        if (extent instanceof Chunk) {
            world = ((Chunk) extent).getWorld();
        } else {
            world = (World) extent;
        }
        List<IFlagSet> flagSetList = new LinkedList<>();
        for(IRegion region : FoxGuardManager.getInstance().regions.get(world)){
            if(region.isInRegion(loc)){
                region.getFlagSets().stream()
                        .filter(flagSet -> !flagSetList.contains(flagSet))
                        .forEach(flagSetList::add);
            }
        }
        Collections.sort(flagSetList);
        int currPriority = flagSetList.get(0).getPriority();
        FlagState flagState = FlagState.PASSTHROUGH;
        for(IFlagSet flagSet : flagSetList){
            if (flagSet.getPriority() < currPriority && flagState != FlagState.PASSTHROUGH){
                break;
            }
            flagState = FlagState.newState(flagState, flagSet.hasPermission(player, typeFlag));
            currPriority = flagSet.getPriority();
        }
        flagState = FlagState.newState(flagState, FlagState.TRUE);
        if(flagState == FlagState.FALSE){
            player.sendMessage(Texts.of("You don't have permission."));
            event.setCancelled(true);
        }
    }
}
