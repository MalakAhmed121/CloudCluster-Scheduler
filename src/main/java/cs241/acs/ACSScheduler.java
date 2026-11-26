package cs241.acs;

import cs241.model.GanttEntry;
import cs241.model.InputValidator;
import cs241.model.Process;
import cs241.model.Scheduler;
import cs241.model.SimulationResult;
import cs241.model.StepSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ACSScheduler implements Scheduler {
    public static final String CONTEXT_SWITCH = "CS";
    public static final String IDLE = "IDLE";

    @Override
    public SimulationResult run(List<Process> processes, int quantum, int contextSwitch) {
        if (quantum <= 0)
            throw new IllegalArgumentException("Quantum must be positive");
        if (contextSwitch < 0)
            throw new IllegalArgumentException("Context switch cannot be negative");
        InputValidator.validate(processes);

        SimulationResult res = new SimulationResult();
        RoundRobin tier1 = new RoundRobin(quantum);
        Tier2Queue tier2 = new Tier2Queue();

        // work on copies , as finish and remaining are changing through out the process
        List<Process> work = new ArrayList<>();
        for (Process p : processes) work.add(p.copy());
        res.processes.addAll(work);
        // Sort processes by arrival time
        List<Process> sorted = new ArrayList<>(work);
        sorted.sort(Comparator.comparingInt((Process p) -> p.arrival));

        int index = 0, time = 0, completed = 0;
        final int n = sorted.size();
        Process last = null;   // last process used the CPU

        while (completed < n) {

            // any process enter Tier1
            while (index < n && sorted.get(index).arrival <= time)
                tier1.addProcess(sorted.get(index++));

            if (tier1.hasReadyProcesses() || tier1.getRunning() != null) {
                // Tier 1: tick
                Process current = tier1.dispatch();

                if (last != null && last != current && contextSwitch > 0) {
                    addEntry(res, CONTEXT_SWITCH, time, time + contextSwitch);
                    time += contextSwitch;
                }
                if (current.firstStart < 0) current.firstStart = time;

                current.remaining--;
                addEntry(res, current.id, time, time + 1);
                time++;
                last = current;

                while (index < n && sorted.get(index).arrival <= time)
                    tier1.addProcess(sorted.get(index++));

                switch (tier1.afterCpuTick(current.remaining)) {
                    case FINISHED:
                        current.finish = time;
                        completed++;
                        break;
                    case MOVE_TO_TIER_2:
                        tier2.add(current, time);
                        res.demotionMatrix.add("t=" + time + " | " + current.id
                                + " demoted T1 -> T2 | " + tier1.snapshot()
                                + " | " + tier2.snapshot());
                        break;
                    case CONTINUE:
                        break;
                }
                res.steps.add(new StepSnapshot(time, current.id,
                        tier1.tier1Items(), tier2.tier2Items(null), List.of()));

            } else if (!tier2.isEmpty()) {
                // Tier 2: non-preemptive، + Aging
//                Aging.updatePriorities(tier2, time, res);
//                Process p = tier2.peekNext();
//
//                if (last != null && last != p && contextSwitch > 0) {
//                    addEntry(res, CONTEXT_SWITCH, time, time + contextSwitch);
//                    time += contextSwitch;
//                }
//                time = tier2.execute(p, time, res);   //  Gantt & firstStart & finish
//                last = p;
//                completed++;
                int agingFrom = res.agingLog.size();
                Aging.updatePriorities(tier2, time, res);
                List<String> agingNotes =
                        new ArrayList<>(res.agingLog.subList(agingFrom, res.agingLog.size()));
                Process p = tier2.peekNext();

                if (last != null && last != p && contextSwitch > 0) {
                    addEntry(res, CONTEXT_SWITCH, time, time + contextSwitch);
                    time += contextSwitch;
                }
                // قبل execute عشان الـ process المختارة تفضل في الجدول معلّمة RUNNING
                res.steps.add(new StepSnapshot(time, p.id,
                        List.of(), tier2.tier2Items(p), agingNotes));
                time = tier2.execute(p, time, res);
                last = p;
                completed++;

            } else {
                // IDLE: Jump to arrival
                int next = sorted.get(index).arrival;
                addEntry(res, IDLE, time, next);
                time = next;
                last = null;                       // no CS after IDLE
            }
        }
        return res;
    }

    //
    private static void addEntry(SimulationResult res, String label, int start, int end) {
        int size = res.gantt.size();
        if (size > 0) {
            GanttEntry prev = res.gantt.get(size - 1);
            if (prev.label.equals(label) && prev.end == start) {
                res.gantt.set(size - 1, new GanttEntry(label, prev.start, end));
                return;
            }
        }
        res.gantt.add(new GanttEntry(label, start, end));
    }
}





























