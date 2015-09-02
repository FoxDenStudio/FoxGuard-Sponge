package tk.elektrofuchse.fox.foxguard.listener;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.BreakBlockEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.PlaceBlockEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 */
public class BlockEventListener implements EventListener<ChangeBlockEvent> {

    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        if (!event.getCause().getFirst(Player.class).isPresent()) return;
        ActiveFlags typeFlag;
        if (event instanceof BreakBlockEvent) typeFlag = ActiveFlags.BLOCK_BREAK;
        else if (event instanceof PlaceBlockEvent) typeFlag = ActiveFlags.BLOCK_PLACE;
        else return;
        Player player = (Player) event.getCause().getFirst(Player.class);
        Vector3i loc = event.getTransactions().get(0).getOriginal().getLocation().get().getBlockPosition();
        World world = event.getTransactions().get(0).getOriginal().getLocation().get().getExtent();
        
        List<IFlagSet> flagSetList = new LinkedList<>();
        FoxGuardManager.getInstance().getRegionListAsStream(world).filter(region -> region.isInRegion(loc))
                .forEach(region -> region.getFlagSets().stream()
                        .filter(flagSet -> !flagSetList.contains(flagSet))
                        .forEach(flagSetList::add));
        Collections.sort(flagSetList);
        int currPriority = flagSetList.get(0).getPriority();
        FlagState flagState = FlagState.PASSTHROUGH;
        for (IFlagSet flagSet : flagSetList) {
            if (flagSet.getPriority() < currPriority && flagState != FlagState.PASSTHROUGH) {
                break;
            }
            flagState = FlagState.newState(flagState, flagSet.hasPermission(player, typeFlag));
            currPriority = flagSet.getPriority();
        }
        if (flagState == FlagState.FALSE) {
            player.sendMessage(Texts.of("You don't have permission."));
            event.setCancelled(true);
        }
    }
}
