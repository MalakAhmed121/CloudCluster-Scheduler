package cs241.acs;
import cs241.model.Process;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class RoundRobin {
    public enum Result {
        CONTINUE, FINISHED, MOVE_TO_TIER_2
    }

    private final Queue<Process> ready = new ArrayDeque<>();
    private final int quantum;
    private Process running;
    private int ticksUsed;

    public RoundRobin(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be positive");
        }
        this.quantum = quantum;
    }

    public void addProcess(Process process) {
        Objects.requireNonNull(process, "Process must not be null");
        ready.add(process);
    }

    public Process dispatch() {
        if (running == null) {
            running = ready.poll();
            ticksUsed = 0;
        }
        return running;
    }

    public Result afterCpuTick(int remainingBurst) {
        if (running == null) {
            throw new IllegalStateException("No process is currently running in Tier 1");
        }
        if (remainingBurst < 0) {
            throw new IllegalArgumentException("Remaining burst cannot be negative: " + remainingBurst);
        }
        ticksUsed++;

        if (remainingBurst == 0) {
            running = null;
            ticksUsed = 0;
            return Result.FINISHED;
        }

        if (ticksUsed == quantum) {
            running = null;
            ticksUsed = 0;
            return Result.MOVE_TO_TIER_2;
        }
        return Result.CONTINUE;
    }

    public Process clearRunning() {
        Process previous = this.running;
        this.running = null;
        this.ticksUsed = 0;
        return previous;
    }

    public Process getRunning() {
        return running;
    }

    public boolean hasReadyProcesses() {
        return !ready.isEmpty();
    }

    public boolean isIdle() {
        return running == null && ready.isEmpty();
    }
}