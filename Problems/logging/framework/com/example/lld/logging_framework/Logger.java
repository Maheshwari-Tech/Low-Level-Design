package com.example.lld.logging_framework;

/**
 * Simple Logging Framework Implementation
 *
 * Design Choices:
 * - Singleton Logger for global access.
 * - Log levels: DEBUG, INFO, WARN, ERROR.
 * - Console appender for simplicity.
 * - Thread-safe with synchronized logging.
 */
public class Logger {
    private static Logger instance;
    private LogLevel currentLevel;

    private Logger() {
        this.currentLevel = LogLevel.INFO; // Default level
    }

    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void setLevel(LogLevel level) {
        this.currentLevel = level;
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    private synchronized void log(LogLevel level, String message) {
        if (level.ordinal() >= currentLevel.ordinal()) {
            System.out.println("[" + level + "] " + message);
        }
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
