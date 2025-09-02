package com.mycompany.app;

public class InfoLogProcessor implements LogProcessor{
    private final LogProcessor next;
    private final LogLevel logLevel;

    public InfoLogProcessor(LogProcessor next) {
        this.next = next;
        this.logLevel = LogLevel.INFO;
    }

    @Override
    public void process(LogLevel logLevel, String message) {
        if(this.logLevel == logLevel){
            System.out.println("printing info - " + message);
        }
        else{
            next.process(logLevel, message);
        }
    }
}
