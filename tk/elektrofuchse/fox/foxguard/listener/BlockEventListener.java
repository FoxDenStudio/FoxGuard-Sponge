package tk.elektrofuchse.fox.foxguard.listener;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;
import tk.elektrofuchse.fox.foxguard.flags.util.FlagState;
import tk.elektrofuchse.fox.foxguard.util.DebugHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Fox on 8/16/2015.
 */
public class BlockEventListener implements EventListener<ChangeBlockEvent> {


    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        if (!event.getCause().any(Player.class)) return;
        DebugHelper.printBlockEvent(event);
        ActiveFlags typeFlag;
        if (event instanceof ChangeBlockEvent.Break) typeFlag = ActiveFlags.BLOCK_BREAK;
        else if (event instanceof ChangeBlockEvent.Place) typeFlag = ActiveFlags.BLOCK_PLACE;
        else return;
        Player player = event.getCause().first(Player.class).get();

        FoxGuardMain.getInstance().getLogger().info(player.getName());

        Vector3i loc = event.getTransactions().get(0).getOriginal().getLocation().get().getBlockPosition();
        World world = event.getTransactions().get(0).getOriginal().getLocation().get().getExtent();

        List<IFlagSet> flagSetList = new ArrayList<>();
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
