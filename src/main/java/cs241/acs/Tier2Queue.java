package cs241.acs;

import cs241.model.GanttEntry;
import cs241.model.Process;
import cs241.model.SimulationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tier2Queue {

    private List<Process> queue = new ArrayList<>();
    private Map<Process, Integer> enteredAt = new HashMap<>();

    // Add a process demoted from Tier 1
    public void add(Process p, int time) {
        if (p != null && p.remaining > 0 && !queue.contains(p)) {
            p.effectivePriority = p.priority;
            queue.add(p);
            enteredAt.put(p, time);
        }
    }

    // Check if the queue is empty
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Get waiting processes for Aging
    public List<Process> getWaiting() {
        return Collections.unmodifiableList(queue);
    }

    // Get the time a process entered Tier 2
    public int getEnteredAt(Process p) {
        return enteredAt.getOrDefault(p, -1);
    }

    // Select the process with the highest priority
    // Lower number means higher priority
    public Process peekNext() {

        if (queue.isEmpty()) {
            return null;
        }

        Process best = queue.get(0);

        for (Process p : queue) {

            if (p.effectivePriority < best.effectivePriority) {
                best = p;
            }

            else if (p.effectivePriority == best.effectivePriority) {

                if (enteredAt.get(p) < enteredAt.get(best)) {
                    best = p;
                }

                else if (enteredAt.get(p).equals(enteredAt.get(best))) {

                    if (p.arrival < best.arrival) {
                        best = p;
                    }

                    else if (p.arrival == best.arrival &&
                             p.id.compareTo(best.id) < 0) {
                        best = p;
                    }
                }
            }
        }

        return best;
    }

    // Execute the selected process until it finishes
    public int execute(Process p, int start, SimulationResult res) {

        if (p == null || !queue.contains(p)) {
            return start;
        }

        queue.remove(p);
        enteredAt.remove(p);

        if (p.firstStart == -1) {
            p.firstStart = start;
        }

        int end = start + p.remaining;

        res.gantt.add(new GanttEntry(p.id, start, end));

        p.remaining = 0;
        p.finish = end;

        return end;
    }

    // Get Tier 2 state for the demotion matrix
    public String snapshot() {

        if (queue.isEmpty()) {
            return "Tier 2: [Empty]";
        }

        StringBuilder sb = new StringBuilder("Tier 2: [");

        for (int i = 0; i < queue.size(); i++) {

            Process p = queue.get(i);

            if (i > 0) {
                sb.append(", ");
            }

            sb.append(p.id)
              .append(" (remaining: ")
              .append(p.remaining)
              .append(", Priority: ")
              .append(p.effectivePriority)
              .append(")");
        }

        sb.append("]");

        return sb.toString();
    }
}
