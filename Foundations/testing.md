# Testing and Testability

Good low-level design makes important behavior easy to exercise without a database, network, wall clock, or UI.

## Test layers

- **Unit tests** verify one object or cohesive component and run entirely in memory.
- **Integration tests** verify an adapter boundary such as persistence, messaging, or a provider client.
- **End-to-end tests** verify a few critical workflows through the assembled application.

Most domain rules should be covered by fast unit tests; do not force every test through the top-level API.

## Arrange, act, assert

Build a readable starting state, perform one behavior, and assert both the result and the meaningful state change. Test names should describe the rule, for example `cannotConfirmExpiredReservation`.

## Test doubles

- A **stub** supplies controlled answers.
- A **fake** is a lightweight working implementation, such as an in-memory repository.
- A **spy** records calls for later inspection.
- A **mock** verifies an expected interaction.

Prefer state assertions and small fakes for domain behavior. Mock only an actual collaboration boundary; excessive interaction testing couples tests to implementation order.

## Design for control

Inject `Clock`, ID generation, randomness, repositories, and external gateways. Depend on focused interfaces so tests can replace the adapter without changing production behavior. This is dependency inversion for a concrete purpose, not a framework requirement.

## Essential cases

For each LLD problem, cover:

1. The primary success workflow.
2. Every invalid state transition.
3. Boundaries such as zero, capacity, expiry, and rounding.
4. Duplicate/idempotent commands.
5. Concurrent access to the same scarce resource.
6. Failure before and after an external side effect.
7. Audit/history correctness.

JUnit is the test runner and assertion ecosystem for Java; Mockito can isolate a boundary. PowerMock-style interception is usually a signal that static construction or hidden dependencies should be redesigned.
