package cs241.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class InputValidator {
    private InputValidator() {}

    public static void validate(List<Process> processes) {
        Objects.requireNonNull(processes, "Processes must not be null");
        if (processes.isEmpty()) throw new IllegalArgumentException("Add at least one process");
        Set<String> ids = new HashSet<>();
        for (Process p : processes) {
            Objects.requireNonNull(p, "Process list cannot contain null");
            if (p.id == null || p.id.isBlank() || p.id.equals("CS") || p.id.equals("IDLE"))
                throw new IllegalArgumentException("Process ID must be non-empty and cannot be CS or IDLE");
            if (!ids.add(p.id))
                throw new IllegalArgumentException("Process IDs must be unique: " + p.id);
            if (p.arrival < 0)
                throw new IllegalArgumentException("Arrival time cannot be negative: " + p.id);
            if (p.burst <= 0)
                throw new IllegalArgumentException("Burst time must be positive: " + p.id);
        }
    }
}