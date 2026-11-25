package cs241.acs;

import cs241.model.Scheduler;
import cs241.model.SimulationResult;
import cs241.model.Process;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class ACSScheduler implements Scheduler {
    @Override
    public SimulationResult run(List<Process> processes, int quantum, int contextSwitch) {

        // Check that the quantum is positive
        if (quantum <= 0) {
            throw new IllegalArgumentException(
                    "Quantum must be positive");
        }
        
        // Check that the context switch time is not negative
        if (contextSwitch < 0) {
            throw new IllegalArgumentException(
                    "Context switch cannot be negative");
        }

        RoundRobin tier1 = new RoundRobin(quantum);
        Tier2Queue tier2 = new Tier2Queue();

        // Sort processes by arrival time
        List<Process> sortedProcesses = new ArrayList<>(processes);
        sortedProcesses.sort(Comparator.comparingInt(p -> p.arrival));

        // Track the next process to arrive
        int index = 0;
        int currentTime = 0;
        int completed = 0;
        int n = sortedProcesses.size();

        Process lastProcess = null;// to know when to make context switch

        while (completed < n) {

            // check the arrival process and add the newly arrival process to tier1
            // check if the arrival time is less than or equal current time
            while (index < n && sortedProcesses.get(index).arrival <= currentTime) {

                Process p = sortedProcesses.get(index);

                tier1.addProcess(p);
                // Move to the next process
                index++;

            }

            if (tier1.hasReadyProcesses() || tier1.getRunning() != null) {

                // Select the process
                Process current = tier1.dispatch();

                if (current != null) {

                    // Check if a context switch is needed
                    if (lastProcess != null && lastProcess != current) {
                        currentTime += contextSwitch;
                    }
                    // Execute one CPU tick
                    current.remaining--;
                    currentTime++;

                    // Save the last process that used the CPU
                    lastProcess = current;

                    //the result after the CPU tick
                    RoundRobin.Result result = tier1.afterCpuTick(current.remaining);

                    switch (result) {
                        case CONTINUE:
                            // Continue executing the same process
                            break;

                        case FINISHED:
                            // Increase the number of completed processes
                            completed++;
                            break;

                        case MOVE_TO_TIER_2:
                            tier2.add(current, currentTime);
                            // Move the process to Tier 2
                            break;

                    }

                }
            }
             else
                currentTime++;
        }
    }
}
