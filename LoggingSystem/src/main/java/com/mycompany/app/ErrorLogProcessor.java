package com.mycompany.app;

public class ErrorLogProcessor implements LogProcessor{
    private final LogProcessor next;
    private final LogLevel logLevel;

    public ErrorLogProcessor(LogProcessor next) {
        this.next = next;
        this.logLevel = LogLevel.ERROR;
    }

    @Override
    public void process(LogLevel logLevel, String message) {
        if(this.logLevel == logLevel){
            System.out.println("printing error - " + message);
        }
        else{
            next.process(logLevel, message);
        }
    }
}
