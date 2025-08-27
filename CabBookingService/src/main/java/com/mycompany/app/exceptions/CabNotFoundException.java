package com.mycompany.app.exceptions;

public class CabNotFoundException extends RuntimeException{

    public CabNotFoundException(String message){
        super(message);
    }
}
