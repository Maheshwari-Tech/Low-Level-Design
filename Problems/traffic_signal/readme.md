# Traffic Signal Control System

## Problem

Implement a simple timed traffic-light state machine, then identify how it would grow into a safe controller for a multi-road intersection with pedestrian and emergency priority.

## Implemented Scope

- One signal cycling `RED -> GREEN -> YELLOW -> RED`.
- Configurable in-memory durations for each color.
- A common `SignalState` contract with `display`, `getDuration`, and `next`.
- Console output and blocking `Thread.sleep` transitions.

`TrafficSignal` owns the current state. `RedState`, `GreenState`, and `YellowState` are inner classes, so the small demo remains encapsulated in one source file. State objects avoid a large conditional and make the transition graph explicit.

## Current Trade-offs

- `Thread.sleep` blocks the controlling thread; a scheduler and monotonic clock are better for non-blocking, testable transitions.
- The cycle is hardcoded rather than loaded as a validated timing plan.
- A single light cannot enforce conflict rules across an intersection.
- The infinite loop stops only when its thread is interrupted.

## Intersection-Level Extension

The broader design introduces:

- **`TrafficController`** as the central coordinator;
- **`Intersection`** owning `Map<Direction, TrafficLight>`;
- one **`TrafficLight`** state machine per controlled movement;
- **`PedestrianCrossing`** with request and clearance phases;
- **`EmergencyHandler`** with a priority queue and safe preemption rules;
- a **`TimingStrategy`** for normal, peak-hour, sensor-adaptive, or manual plans.

Required safety invariants include never granting green to conflicting movements, inserting yellow/all-red clearance before switching right-of-way, bounding pedestrian wait time, and making emergency/manual override transitions explicit and auditable.

The current **State** pattern remains useful per light. **Strategy** can choose timing plans, and **Observer** can deliver sensor/emergency events to the controller; the latter two are planned, not implemented.

## Implementation Status

The single-signal console cycle is implemented. Multiple directions, intersection conflict matrices, pedestrian phases, emergency priority, peak-hour adjustment, manual override, sensor input, durable configuration, and a GUI are extensions.

## Run

From `Problems/traffic_signal`:

```bash
javac com/example/lld/traffic_signal/TrafficSignal.java
java com.example.lld.traffic_signal.TrafficSignal
```

Interrupt the process to stop the continuous cycle.
