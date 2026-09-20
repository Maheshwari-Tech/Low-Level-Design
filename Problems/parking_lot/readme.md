# Parking Lot System

## Problem

Design an automated parking lot. The baseline acceptance problem is a single lot of numbered car slots that always assigns the nearest available slot and supports regulatory lookups. A multi-floor, multi-vehicle, billed facility is a deliberate extension of that baseline.

## Baseline Requirements

- Create a lot with slots numbered from 1 in increasing distance from the entrance.
- Park a car using registration number and color; return the allocated slot.
- Reject parking when the lot is full.
- Leave a numbered slot and make it reusable.
- Display occupied slots in slot-number order.
- Find registrations by color.
- Find slots by color.
- Find a slot by registration number.
- Accept the same commands interactively or from an input file.

The full command contract and expected output are in [`problem-statement.md`](problem-statement.md). Commands include `create_parking_lot`, `park`, `leave`, `status`, the three lookup commands, and `exit`.

## Baseline Model

- **`ParkingLot`**: owns numbered `ParkingSpot` instances.
- **`ParkingSpot`**: slot ID and optional parked vehicle.
- **`Vehicle`**: registration number and color.
- **`ParkingLotService`**: parking, leaving, status, and lookup use cases.
- **`ParkingStrategy`**: selects an available slot.
- **`NaturalNumberStrategy`**: chooses the lowest available slot number.
- **`CommandExecutor`** and **`CommandExecutorFactory`**: parse and dispatch CLI commands.
- **`OutputPrinter`**: centralizes exact console output.

## Data Structures

- `Map<Integer, ParkingSpot>` for direct slot lookup.
- `Map<String, Integer>` from registration to slot.
- `Map<String, Set<Integer>>` from normalized color to sorted slots.
- A min-heap or ordered set of available slot IDs for O(log n) nearest-slot allocation; a linear natural-number scan is simpler but O(n).

Parking and leaving must update the spot and every secondary index atomically. A registration should occupy at most one slot, and an occupied slot cannot be allocated twice.

## Patterns

- **Strategy** for nearest-slot or future allocation policies.
- **Command** for interactive and file-driven input through the same execution path.
- **Factory** for resolving command executors.
- A Singleton is not required; explicit ownership makes the service and printer easier to test.

## Multi-Floor and Billing Extension

The broader domain can add:

- **`Floor`** and vehicle-specific **`SpotType`** values for cars, motorcycles, and trucks;
- **`Ticket`** containing entry time, vehicle, floor, and spot;
- a floor/type availability index such as `Map<SpotType, Queue<Spot>>`;
- **`PricingStrategy`** for hourly, flat, lost-ticket, or vehicle-specific rates;
- **`Payment`** for charge, payment, and receipt state;
- **Observer** notifications for capacity thresholds or newly available spots.

These extensions should preserve the baseline command contract rather than mixing billing concerns into nearest-slot allocation.

## Implementation Status

The nearest-slot baseline is implemented in [`com/example/lld/parking_lot`](com/example/lld/parking_lot). The service allocates the lowest free slot, prevents duplicate registrations, releases slots, reports occupied spots in order, and maintains color/registration lookups. `OutputPrinter`, the command enum, and executor registry are present; a text parser, concrete command executors, file input, and `exit` handling are not yet wired into the demo. Multi-floor support, vehicle-type spots, tickets, real-time availability, and automated billing remain design extensions.

```bash
javac -d /tmp/lld-build Problems/parking_lot/com/example/lld/parking_lot/*.java Problems/parking_lot/com/example/lld/parking_lot/commands/*.java
java -cp /tmp/lld-build com.example.lld.parking_lot.Main
```
