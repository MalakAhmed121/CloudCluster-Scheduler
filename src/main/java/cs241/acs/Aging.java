
package cs241.acs;

import cs241.model.Process;
import cs241.model.SimulationResult;
import static java.lang.Math.floor;

public class Aging {


    private static final int AGING_INTERVAL = 4;

    public static void updatePriorities(Tier2Queue tier2Queue, int currentTime, SimulationResult result) {

        for (Process p : tier2Queue.getWaiting()) {

            int waiting_time = currentTime - tier2Queue.getEnteredAt(p);

            int newEffectivePriority = Math.max(1, p.priority - (int) floor(waiting_time / (double) AGING_INTERVAL));

            int old_priority = p.effectivePriority;

            if (old_priority != newEffectivePriority) {
                result.agingLog.add(
                        p.id + ": Priority changed from " +
                                old_priority + " to " + newEffectivePriority);
            }

            p.effectivePriority = newEffectivePriority;
        }
    }

}