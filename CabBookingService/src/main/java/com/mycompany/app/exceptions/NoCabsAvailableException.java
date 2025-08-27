package com.mycompany.app.exceptions;

public class NoCabsAvailableException extends RuntimeException{

    public NoCabsAvailableException(String message){
        super(message);
    }
}
