# Robotic Vacuum Cleaner

## Prompt and scope

Design a vacuum controller for an `m x n` room. Initialize the robot at `(x, y)` facing north, south, east, or west; accept `L`, `R`, and `M` instructions; and return the current pose after each command. The source provides a grid, pose, move request, validator boundary, and template-method-style execution pipeline.

Path planning, dirt sensing, battery charging, mapping, and multiple robots are follow-ups.

## Core model

- `Cell`: coordinates and current direction in the source; a canonical design separates immutable `Position` and `Direction` from room cells.
- `DirectionEnum`: `N`, `S`, `E`, and `W`.
- `MoveRequest`: current pose plus one instruction.
- `VacuumService`: owns the room/current cell and interprets commands.
- `ParameterValidator`: validates instructions and coordinates.
- `BizTemplate` / `BizBaseCallback`: pre-process, process, and post-process workflow.

## Invariants

- `0 <= x < rows` and `0 <= y < columns`; there is exactly one current robot pose.
- `L` and `R` rotate 90 degrees without changing coordinates.
- `M` moves exactly one cell in the current direction and never crosses a wall or obstacle.
- Only `L`, `R`, and `M` are accepted; a rejected instruction leaves state unchanged.
- Room dimensions and robot state belong to one controller instance, not shared static globals.

## API

```text
createRoom(rows, columns, startPosition, direction) -> VacuumController
instruct(command, commandId) -> Pose
execute(sequence, commandId) -> ExecutionResult
currentPose() -> Pose
```

The source has private constructors and a private `instruct(MoveRequest)` method; a canonical API makes creation and execution explicit and testable.

## Main flow

1. Validate room dimensions and initial pose, then initialize controller state.
2. Parse one instruction and validate it before mutation.
3. Compute the candidate direction or coordinate.
4. Reject an invalid move or atomically replace the current pose.
5. Return an immutable pose and record command outcome for retry safety.

## Concurrency and failure handling

- Reject invalid dimensions, instructions, out-of-bounds moves, obstacles, stale expected versions, and duplicate command IDs.
- Serialize commands for one robot or guard the pose with optimistic compare-and-set; commands for different robots can proceed independently.
- Do not swallow validation or execution exceptions. A failed command must not partially mutate state.
- The source's static room/current cell causes instances to interfere and is unsuitable for concurrency.

## Design solution

Use a `VacuumController` aggregate containing a `Room` and immutable `Pose`. Direction owns `turnLeft`, `turnRight`, and movement deltas, removing a central switch. A command handler performs validate-compute-commit and returns a typed result. The source callback template can host cross-cutting validation/audit, but it should not hide failures.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Incomplete canonical fragment | [`References/kumaransg-LLD/Low_level_Design_Problems/vacuumcleaner`](../../References/kumaransg-LLD/Low_level_Design_Problems/vacuumcleaner/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/vacuumcleaner) | Controller, request, callback template, and empty validators. |
| **Exact duplicate** | [`References/kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/vacuumcleaner`](../../References/kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/vacuumcleaner/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Low-Level-Design-1/vacuumcleaner) | File-for-file copy of the canonical fragment; not a second approach. |

## Follow-ups

- Add obstacles, dirt levels, sensors, battery state, charging, and coverage planning.
- Support pause/resume, durable command logs, and recovery after restart.
- Compare random walk, wall following, spiral, and mapped coverage strategies.
- Coordinate multiple cleaners while preventing cell collisions.

## Implementation status

**Non-functional reference fragment; no code copied here.** The source compiles, but its main throws a null-pointer exception because the constructor reads the static room before initializing it. Validation methods are empty, movement logic is unfinished, exceptions are swallowed, and the result pose is never updated meaningfully.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
