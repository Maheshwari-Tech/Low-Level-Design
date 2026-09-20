# Elevator System

## Problem

Design a controller for multiple elevators serving a multi-floor building. It must accept hall and car requests, select an elevator, move it safely, and expose clear door and emergency states.

## Functional Requirements

- Support multiple elevators and floors.
- Accept external `(floor, direction)` requests and internal destination selections.
- Assign requests with a replaceable scheduling policy.
- Process stops in a direction-aware order and avoid duplicate work.
- Model movement and door opening, open, closing, and idle states.
- Validate floor and elevator identifiers.
- Provide status for every elevator.
- Define emergency-stop behavior; recovery, alarms, overload detection, and fire mode are extensions.

## Core Model

- **`ElevatorSystem`**: owns elevators and floors, receives requests, and chooses a car.
- **`Elevator`**: current floor, direction, state, and pending internal stops.
- **`Floor`**: floor number and up/down hall requests.
- **`ElevatorController`**: movement, door, and emergency commands.
- **`Request`**: requested floor and direction.
- **`Direction`**: `UP`, `DOWN`, or `IDLE`.
- **`ElevatorState`**: idle, movement, door, and emergency states.
- **`SchedulingStrategy`**: planned abstraction for nearest-car, collective-control, or other policies.

## Data Structures

- `List<Elevator>` for the fleet.
- `Map<Integer, Floor>` for floor lookup.
- Ordered queues or two priority queues per elevator for upward and downward stops.
- FIFO queues on each floor when individual hall requests must be retained.

A single ascending `PriorityQueue<Integer>` is simple, but it does not correctly prioritize downward travel. Production scheduling should maintain direction-specific stop sets.

## Patterns

- The implementation uses an explicit `ElevatorState` model; a full **State** pattern could move state-specific commands into behavior objects.
- **Strategy** is the planned boundary for replaceable scheduling and assignment; nearest-car selection is currently embedded in `ElevatorSystem`.
- **Command** can represent requests or controller actions that need queuing, audit, or retry.
- **Observer** can update panels, displays, alarms, and monitoring.

## Request Flow

1. Validate the floor and requested direction.
2. Create a hall request and run the assignment policy (embedded nearest-car logic today) to choose an elevator.
3. Add the stop without duplicating an existing request.
4. Move through ordered stops, opening and closing doors at each arrival.
5. Publish status and continue in the current direction until its queue is exhausted.

## Implementation Status

The reference model is implemented in [`com/example/lld/elevator_system`](com/example/lld/elevator_system). It validates requests, assigns the closest compatible elevator, models movement/door/emergency states, deduplicates internal stops, and uses an injectable zero-delay simulator so the demo is deterministic and non-blocking. Nearest-car selection is currently embedded in `ElevatorSystem`; a replaceable scheduling strategy, direction-specific stop queues, overload/fire recovery, and asynchronous dispatch remain extensions.

```bash
javac -d /tmp/lld-build Problems/elevator_system/com/example/lld/elevator_system/*.java
java -cp /tmp/lld-build com.example.lld.elevator_system.Main
```
