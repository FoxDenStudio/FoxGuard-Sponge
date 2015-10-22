package tk.elektrofuchse.fox.foxguard.util;

import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.CauseTracked;
import tk.elektrofuchse.fox.foxguard.FoxGuardMain;

/**
 * Created by Fox on 10/21/2015.
 */
public class DebugHelper {

    public static void printCauses(CauseTracked event) {
        StringBuilder sb = new StringBuilder().append("\n");
        for (Object o : event.getCause().all()) {
            sb.append(o).append("\n");
        }
        FoxGuardMain.getInstance().getLogger().info(sb.toString());
    }

    public static void printEvent(Event event) {
        StringBuilder sb = new StringBuilder().append("-----------------------------------\n");
        sb.append(event.getClass()).append("\n\n");
        if (event instanceof CauseTracked) {
            for (Object o : ((CauseTracked) event).getCause().all()) {
                sb.append(o).append("\n");
            }
        }
        FoxGuardMain.getInstance().getLogger().info(sb.toString());
    }

    public static void printBlockEvent(ChangeBlockEvent event) {
        StringBuilder sb = new StringBuilder().append("-----------------------------------\n");
        sb.append(event.getClass()).append("\n");
        for(Transaction t : event.getTransactions()){
            sb.append(t).append("\n");
        }
        sb.append("\n");
        for (Object o : event.getCause().all()) {
            sb.append(o).append("\n");
        }
        FoxGuardMain.getInstance().getLogger().info(sb.toString());
    }
}
