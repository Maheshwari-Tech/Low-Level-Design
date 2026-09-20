# Logging Problems

This directory contains two different logging implementations demonstrating various design patterns and system designs.

## Framework Implementation

**Location**: `framework/`

**Problem Statement**: Implement a simple console logging framework with different log levels, a singleton for global access, and a configurable severity threshold. Multiple appenders and configurable output routing are documented extensions in `framework/README.md`.

**Key Features**:
- Singleton logger instance
- Configurable log levels
- Thread-safe logging
- Console output

**Design Patterns Used**: Singleton

## System Implementation

**Location**: `system/`

**Problem Statement**: Design a logging system with multiple processors that can handle different types of logs (e.g., database logs, file logs, null logs). The system should allow chaining processors and conditional processing based on log levels or types.

**Key Features**:
- Chain of responsibility for log processing
- Multiple log destinations
- Null object pattern for no-op processing
- Extensible processor architecture

**Design Patterns Used**: Chain of Responsibility, Null Object
