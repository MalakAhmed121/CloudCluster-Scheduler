# CloudCluster-Scheduler

**A CPU Scheduling Simulator for Cloud-Cluster Environments**

CloudCluster-Scheduler is a Java-based project that simulates CPU scheduling strategies for different types of processes, focusing on efficient resource allocation and scheduling performance.

## Features

- **Preemptive SJF:** Shortest Remaining Time First scheduling.
- **Round Robin:** Time Quantum-based process scheduling.
- **Priority Scheduling:** Non-Preemptive Priority scheduling.
- **Aging:** Dynamic priority updates to prevent starvation.
- **Context Switching:** Simulation of context switching overhead.
- **Performance Analysis:** Waiting, Turnaround, and Response Time calculations.
- **Demotion Matrix:** Tracking process movements between scheduling tiers.
- **GUI (Bonus):** Interactive scheduling visualization using JavaFX.

## Scheduling Architecture

```text
          CloudCluster Scheduler
                   |
          +--------+--------+
          |                 |
       Preemptive          ACS
          SJF               |
                       +----+----+
                       |         |
                     Tier 1    Tier 2
                       RR      Priority
                                 |
                              Tier 3
                               Aging
```

## Tech Stack

- Java
- JavaFX
- Git & GitHub

## Project Structure

```text
CloudCluster-Scheduler/
│
├── src/
      └─ main/
          └─  java
                └─ cs241
                   ├─ App
                   ├─ model
                   │   ├─ Process
                   │   ├─ GanttEntry
                   │   ├─ SimulationResult
                   │   └─ Scheduler
                   ├─ sjf
                   │   └─ PreemptiveSJF
                   └─ acs
                       └─ ACSScheduler  
├── gui/
├── docs/
└── README.md
```

## Team

Developed by a team of 5 students.

| Member | Module |
|---|---|
| Person 1 | Preemptive SJF |
| Person 2 | ACS – Tier 1 |
| Person 3 | ACS – Tier 2 |
| Person 4 | ACS – Tier 3 (Aging) |
| Person 5 | Shared Foundation & Integration |

## Academic Information

**Course:** CS241 – Operating Systems 1

**Assignment:** Assignment 2

**Faculty:** Faculty of Computers and Artificial Intelligence

**University:** Cairo University

**Semester:** Fall 2026

---

*Developed as part of the Operating Systems course project.*
