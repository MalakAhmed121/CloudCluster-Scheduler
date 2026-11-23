package cs241.model;
import java.util.List;
public interface Scheduler {
    SimulationResult run(List<Process> processes, int quantum, int contextSwitch);
}
