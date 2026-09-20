# Proxy Pattern

## Intent

Place a stand-in with the same interface in front of another object to control how and when that object is accessed.

## When to use

- Access requires authorization, validation, rate limiting, caching, or audit logging.
- An expensive or remote object should be created or contacted lazily.
- Client code should remain unaware of the access-control mechanism.

## Participants and mechanics

- **Subject** is the interface shared by the proxy and real subject.
- **Real subject** performs the underlying work.
- **Proxy** holds or locates the real subject, applies policy, and delegates allowed calls.
- The client receives a subject and therefore does not need a separate proxy-specific workflow.

## Trade-offs

- Centralizes cross-cutting access policy without changing the real subject.
- Adds latency and another failure point; hidden remote calls or lazy loading can surprise clients.
- A proxy must preserve the subject's behavioral contract, including errors and return values.

## Implementation status

**Runnable.** [`EmployeeDaoProxy.java`](EmployeeDaoProxy.java) guards mutations and delegates to [`EmployeeDaoImpl.java`](EmployeeDaoImpl.java) through the [`EmployeeDao`](EmployeeDao.java) interface. [`Demo.java`](Demo.java) exercises unrestricted reads plus allowed and denied mutations.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/proxy/*.java
java -cp "$out" code.structural.proxy.Demo
```
