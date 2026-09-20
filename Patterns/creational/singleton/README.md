# Singleton Pattern

## Intent

Ensure a class has one controlled instance and provide a well-defined way to access it.

## When to use

- The process truly requires one coordinator or shared resource owner.
- Construction and lifetime must be controlled centrally.
- A framework cannot manage the object's lifecycle more explicitly through dependency injection.

## Participants and mechanics

- The **singleton** hides its constructor and stores its instance in a static field.
- A static access method creates or returns that instance.
- Concurrent lazy initialization must use a correct synchronization strategy.
- Clients use the access method rather than calling a constructor.

## Trade-offs

- Enforces the instance constraint and supports lazy creation.
- Introduces global state, hides dependencies, complicates isolation in tests, and can become a concurrency bottleneck.
- Eager initialization or an enum is often simpler than hand-written lazy locking in Java.

## Implementation status

**Runnable.** [`SingletonNaiveButCorrect.java`](SingletonNaiveButCorrect.java) uses eager initialization, while [`SingletonPattern.java`](SingletonPattern.java) demonstrates lazy initialization with volatile double-checked locking. [`SingletonDemo.java`](SingletonDemo.java) verifies identity for both implementations. The older lazy example retains its historical `patterns.creational.singleton` package.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/creational/singleton/*.java
java -cp "$out" code.creational.singleton.SingletonDemo
```
