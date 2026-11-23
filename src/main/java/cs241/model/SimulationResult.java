package cs241.model;

import java.util.ArrayList;
import java.util.List;
public class SimulationResult {
    public final List<Process> processes = new ArrayList<>();
    public final List<GanttEntry> gantt = new ArrayList<>();
    public final List<String> demotionMatrix = new ArrayList<>();
    public final List<String> agingLog = new ArrayList<>();    //person 4
}
