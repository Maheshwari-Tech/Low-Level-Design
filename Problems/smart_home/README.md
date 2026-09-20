# Smart Home

## Prompt and scope

Design an in-memory smart-home controller that registers voice hubs and appliances, connects appliances to a hub, applies on/off and level commands, lists a hub's connected appliances, and reports per-appliance usage. The source models Google and Alexa hubs; lights, fans, and a charger; and brightness or speed ranges.

Keep device discovery, remote networking, authentication, schedules, and automation rules out of the first iteration.

## Core model

- `Device`: voice hub identity, `DeviceType`, `ActivationType`, and connected `SmartHomeDevice` references.
- `SmartHomeDevice`: appliance identity, category, owning hub, power state, usage count, and `StateProperty`.
- `StateProperty`: capability (`BRIGHTNESS` or `SPEED`) with minimum, maximum, and current level.
- `Command`: target hub and appliance, desired power state, and requested level.
- `HomeService`: application boundary over hub/appliance repositories.
- `DeviceDao`, `SmartHomeDeviceDao`, and `LocalDbHashMap`: source persistence abstractions and in-memory maps.

## Invariants

- Hub and appliance IDs are unique and stable; an appliance is connected to exactly one existing hub.
- The hub's connected-device list and the appliance's `connectedToDeviceId` agree.
- A requested property matches the appliance capability and `minLevel <= currentLevel <= maxLevel`.
- A command updates power, level, and usage atomically. Repeating a command should have a defined idempotent result.
- Usage has one explicit unit. The source increments it once on an on-to-off transition; a production design should instead define elapsed time or energy.

## API

The source-facing service contract is:

```text
addDevice(device)
addSmartHomeDevice(appliance)
sendCommand(command)
getConnectedSmartHomeDeviceList(hubId)
getDeviceUsageDetails()
```

A complete version should add an explicit `connect(applianceId, hubId)` operation and return typed results rather than exposing mutable domain objects.

## Main flow

1. Register a hub and an appliance, then connect them.
2. Resolve both IDs when a command arrives and verify membership.
3. Validate the requested capability and range.
4. Apply the power and level transition as one write.
5. Update usage according to the chosen accounting rule and return the new appliance snapshot.

## Concurrency and failure handling

- Reject unknown hubs/appliances, invalid capabilities, out-of-range levels, and conflicting connections.
- The source uses static `HashMap` instances and generates IDs from `size() + 1`; neither is safe under concurrent registration or deletion.
- Serialize commands per appliance or use optimistic versions so concurrent state changes cannot overwrite one another.
- Do not return internal mutable lists. Preserve command IDs for retry deduplication and audit failures separately from device state.

## Design solution

Use `Home` or `Hub` as the aggregate boundary, repository interfaces for persistence, and a command handler that resolves and validates targets before invoking appliance behavior. Model capabilities as typed policies instead of a single nullable property bag. Publish a state-change event after a successful commit for usage aggregation and automation rules.

The source's service/DAO separation is a useful boundary, but connection management and ID assignment belong in the domain/repository transaction rather than in unrelated mutable maps.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Partial extracted implementation | [`References/kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome`](../../References/kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/smartHome/com.flipkart.smartHome) | Domain, DAO, service, compiled output, and an incomplete driver. |
| **Exact duplicate archive** | [`References/kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome.zip`](../../References/kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome.zip) | [ZIP at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/smartHome/com.flipkart.smartHome.zip) | Same source as the extracted tree; not a second solution. |

## Follow-ups

- Add rooms, users, permissions, scenes, schedules, and automation rules.
- Support capability discovery and appliance-specific commands without central switches.
- Track energy and uptime from timestamped state changes.
- Define offline-device queues, acknowledgements, retries, and reconciliation.
- Persist an audit log and expose current state through immutable read models.

## Implementation status

**Reference-backed question; no code copied here.** The extracted source compiles and its `HomeServiceImplTest` main exits, but the driver never sends a command. Repository keys are not assigned back to objects, `addSmartHomeDevice` does not attach an appliance to a hub, and the demonstrated flow is not end-to-end functional.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
