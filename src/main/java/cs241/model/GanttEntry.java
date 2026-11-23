package cs241.model;

public class GanttEntry {
    public final String label;
    public final int start , end;
    public GanttEntry(String label, int start, int end) {
        this.label = label;
        this.start = start;
        this.end = end;
    }
}
