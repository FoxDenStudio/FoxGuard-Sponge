package tk.elektrofuchse.fox.foxguard.listener;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;
import tk.elektrofuchse.fox.foxguard.FoxGuardManager;
import tk.elektrofuchse.fox.foxguard.flags.IFlagSet;
import tk.elektrofuchse.fox.foxguard.flags.util.ActiveFlags;

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
        //DebugHelper.printBlockEvent(event);
        ActiveFlags typeFlag;
        if (event instanceof ChangeBlockEvent.Modify) typeFlag = ActiveFlags.BLOCK_MODIFY;
        else if (event instanceof ChangeBlockEvent.Fluid) typeFlag = ActiveFlags.FLUID;
        else if (event instanceof ChangeBlockEvent.Break) typeFlag = ActiveFlags.BLOCK_BREAK;
        else if (event instanceof ChangeBlockEvent.Place) typeFlag = ActiveFlags.BLOCK_PLACE;
        else return;
        Player player = event.getCause().first(Player.class).get();

        FoxGuardMain.getInstance().getLogger().info(player.getName());

        List<IFlagSet> flagSetList = new ArrayList<>();
        World world = event.getTargetWorld();

        for (Transaction<BlockSnapshot> trans : event.getTransactions()) {
            Vector3i loc = trans.getOriginal().getLocation().get().getBlockPosition();
            FoxGuardManager.getInstance().getRegionListAsStream(world).filter(region -> region.isInRegion(loc))
                    .forEach(region -> region.getFlagSets().stream()
                            .filter(flagSet -> !flagSetList.contains(flagSet))
                            .forEach(flagSetList::add));
        }
        Collections.sort(flagSetList);
        int currPriority = flagSetList.get(0).getPriority();
        Tristate flagState = Tristate.UNDEFINED;
        for (IFlagSet flagSet : flagSetList) {
            if (flagSet.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            flagState = flagState.and(flagSet.hasPermission(player, typeFlag));
            currPriority = flagSet.getPriority();
        }
        if (flagState == Tristate.FALSE) {
            player.sendMessage(Texts.of("You don't have permission."));
            event.setCancelled(true);
        }
    }


}
