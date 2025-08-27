package com.mycompany.app.exceptions;

public class CabsAlreadyExistsException extends RuntimeException{

    public CabsAlreadyExistsException(String message){
        super(message);
    }
}
