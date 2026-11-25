package cs241.sjf;

import cs241.model.Process;
import cs241.model.Scheduler;
import cs241.model.SimulationResult;
import cs241.model.GanttEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Shortest Remaining Time First scheduler with explicit context-switch intervals. */
public class PreemptiveSJF implements Scheduler {
    public static final String CONTEXT_SWITCH = "CS";
    public static final String IDLE = "IDLE";

    @Override
    public SimulationResult run(List<Process> processes, int quantum, int contextSwitch) {
        Objects.requireNonNull(processes, "Processes must not be null");
        if (contextSwitch < 0) {
            throw new IllegalArgumentException("Context switch cannot be negative");
        }

        List<Process> ordered = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Process input : processes) {
            Objects.requireNonNull(input, "Process list cannot contain null");
            if (input.id == null || input.id.isBlank()
                    || input.id.equals(CONTEXT_SWITCH) || input.id.equals(IDLE)) {
                throw new IllegalArgumentException("Process ID must be non-empty and cannot be CS or IDLE");
            }
            if (!ids.add(input.id)) {
                throw new IllegalArgumentException("Process IDs must be unique: " + input.id);
            }
            if (input.arrival < 0) {
                throw new IllegalArgumentException("Arrival time cannot be negative: " + input.id);
            }
            if (input.burst <= 0) {
                throw new IllegalArgumentException("Burst time must be positive: " + input.id);
            }
            ordered.add(input.copy());
        }
        ordered.sort(Comparator.comparingInt((Process p) -> p.arrival).thenComparing(p -> p.id));

        SimulationResult result = new SimulationResult();
        result.processes.addAll(ordered);
        int time = 0;
        int completed = 0;
        Process lastExecuted = null;
        boolean switching = false;
        int switchRemaining = 0;

        while (completed < ordered.size()) {
            int now = time;
            Process selected = ordered.stream()
                    .filter(p -> p.arrival <= now && p.remaining > 0)
                    .min(Comparator.comparingInt((Process p) -> p.remaining)
                            .thenComparingInt(p -> p.arrival)
                            .thenComparing(p -> p.id))
                    .orElse(null);

            if (switching) {
                if (selected == null) {
                    append(result, IDLE, time, time + 1);
                    time++;
                    continue;
                }
                append(result, CONTEXT_SWITCH, time, time + 1);
                time++;
                switchRemaining--;
                if (switchRemaining == 0) {
                    switching = false;
                    lastExecuted = null;
                    // The next loop reselects using arrivals during the switch interval.
                }
                continue;
            }

            if (selected == null) {
                append(result, IDLE, time, time + 1);
                time++;
                lastExecuted = null;
                continue;
            }

            if (lastExecuted != null && selected != lastExecuted && contextSwitch > 0) {
                switching = true;
                switchRemaining = contextSwitch;
                continue;
            }

            if (selected.firstStart < 0) {
                selected.firstStart = time;
            }
            append(result, selected.id, time, time + 1);
            selected.remaining--;
            time++;
            if (selected.remaining == 0) {
                selected.finish = time;
                completed++;
            }
            lastExecuted = selected;
        }
        return result;
    }

    private static void append(SimulationResult result, String label, int start, int end) {
        int size = result.gantt.size();
        if (size > 0) {
            GanttEntry previous = result.gantt.get(size - 1);
            if (previous.label.equals(label) && previous.end == start) {
                result.gantt.set(size - 1, new GanttEntry(label, previous.start, end));
                return;
            }
        }
        result.gantt.add(new GanttEntry(label, start, end));
    }
}
