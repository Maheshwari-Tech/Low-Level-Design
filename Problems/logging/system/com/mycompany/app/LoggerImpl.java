package com.mycompany.app;

public class LoggerImpl implements Logger{
    private final LogProcessor logProcessor;

    public LoggerImpl(LogProcessor logProcessor) {
        this.logProcessor = logProcessor;
    }

    @Override
    public void log(LogLevel logLevel, String message) {
        logProcessor.process(logLevel, message);
    }
}
