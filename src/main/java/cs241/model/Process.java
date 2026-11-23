package cs241.model;

public class Process {
    public final String id;
    public final int arrival, burst, priority;
    public int remaining;
    public int firstStart = -1;
    public int finish;
    public int  effectivePriority;     //person 4

    public Process(String id, int arrival, int burst, int priority) {
        this.id = id;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.remaining = burst;
        this.effectivePriority = priority;
    }
    public int turnaround() { return finish - arrival;}
    public int waiting() { return turnaround() - burst; }
    public int response() { return firstStart - arrival; }

    public Process copy() { return new Process(id, arrival, burst, priority); }

}
