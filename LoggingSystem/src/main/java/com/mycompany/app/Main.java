package com.mycompany.app;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        Logger logger = new LoggerImpl(new InfoLogProcessor(new DebugLogProcessor(new ErrorLogProcessor(new NullEffectLogProcessor()))));

        logger.log(LogLevel.ERROR, "error");
        logger.log(LogLevel.DEBUG, "debug info print in blue");
        logger.log(LogLevel.INFO, "print in green");
        logger.log(LogLevel.ERROR, "error print in red");

    }
}