package com.mycompany.app;

public class DebugLogProcessor implements LogProcessor {
    private final LogProcessor next;
    private final LogLevel logLevel;

    public DebugLogProcessor(LogProcessor next) {
        this.next = next;
        this.logLevel = LogLevel.DEBUG;
    }

    @Override
    public void process(LogLevel logLevel, String message) {
        if(this.logLevel == logLevel){
            System.out.println("printing debug level - " + message);
        }
        else{
            next.process(logLevel, message);
        }
    }
}
