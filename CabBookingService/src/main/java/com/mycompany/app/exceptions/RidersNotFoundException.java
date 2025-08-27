package com.mycompany.app.exceptions;

public class RidersNotFoundException extends RuntimeException{

    public RidersNotFoundException(String message){
        super(message);
    }
}
