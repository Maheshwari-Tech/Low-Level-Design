# Logging Framework

## Problem

Implement a small logging facade with severity filtering, then define an extensible Log4j/Logback-style design for named loggers, formatting, routing, multiple destinations, and asynchronous delivery.

## Implemented Scope

- A lazily initialized singleton `Logger`.
- `DEBUG`, `INFO`, `WARN`, and `ERROR` levels.
- A configurable minimum level using enum ordering.
- Convenience methods for each severity.
- Synchronized instance creation and output.
- Console messages formatted as `[LEVEL] message`.

The only required structures are a static logger reference and a `LogLevel` enum. Calls below the configured threshold are ignored.

## Design Decisions and Trade-offs

- **Singleton** provides simple global access, but dependency injection would improve test isolation and support multiple logger configurations.
- Synchronizing each emitted line prevents interleaved writes in this demo but serializes callers.
- Enum ordinal comparison is concise; explicit severity values are safer if enum declaration order changes.
- Console formatting is fixed and messages contain no timestamp, logger name, context, or exception payload.

## Extensible Framework Model

- **`LoggerFactory`**: returns/reuses named loggers from `Map<String, Logger>`.
- **`LogEvent`**: immutable timestamp, level, logger name, message, throwable, and context.
- **`Appender`**: destination interface with console, file, and database implementations.
- **`Layout` / `Formatter`**: renders an event independently of its destination.
- **`Filter`** and routing rules: decide which appenders receive an event.
- **`AsyncAppender`**: places events on a bounded `Queue<LogEvent>` and defines overflow/shutdown behavior.

A logger may own `List<Appender>`. **Factory** manages named loggers, **Observer** models distribution to appenders, and **Decorator** can wrap appenders with async, filtering, buffering, or retry behavior. These patterns and components describe the planned framework; the current source implements only Singleton and console output.

## Operational Requirements for Extensions

- Define ordering and whether callers block, drop, or spill when the async queue is full.
- Flush accepted events during graceful shutdown.
- Isolate an appender failure from other destinations.
- Add file rotation/retention and safe resource cleanup.
- Make formatting, thresholds, filters, and routes configurable.
- Avoid recursively logging failures from inside the logging framework.

## Implementation Status

The singleton console logger and severity threshold are implemented. Named loggers, logger registry, events, appenders, custom layouts, file/database output, asynchronous queues, rotation, filtering, and routing are not implemented.

## Run

From `Problems/logging/framework`:

```bash
javac com/example/lld/logging_framework/*.java
java com.example.lld.logging_framework.Main
```

At the default `INFO` threshold, the first debug line is filtered; the final debug line appears after the demo switches to `DEBUG`.
