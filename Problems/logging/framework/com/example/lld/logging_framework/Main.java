package com.example.lld.logging_framework;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getInstance();

        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warn("This is a warning message");
        logger.error("This is an error message");

        logger.setLevel(Logger.LogLevel.DEBUG);
        logger.debug("Now debug messages will show");
    }
}
