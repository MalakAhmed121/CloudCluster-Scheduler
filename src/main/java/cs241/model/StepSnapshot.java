package cs241.model;

import java.util.List;

public class StepSnapshot {
    public final int timeStep;
    public final String runningProcessId;
    public final List<Tier1Item> tier1Queue;
    public final List<Tier2Item> tier2Queue;
    public final List<String> agingEvents;

    public StepSnapshot(
            int timeStep,
            String runningProcessId,
            List<Tier1Item> tier1Queue,
            List<Tier2Item> tier2Queue,
            List<String> agingEvents
    ) {
        this.timeStep = timeStep;
        this.runningProcessId = runningProcessId;
        this.tier1Queue = tier1Queue;
        this.tier2Queue = tier2Queue;
        this.agingEvents = agingEvents;
    }

    public record Tier1Item(String processId, int remainingBurst) {}

    public record Tier2Item(String processId, int effectivePriority, int remainingTime, boolean running) {}
}