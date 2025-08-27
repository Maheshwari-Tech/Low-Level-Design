package com.mycompany.app.exceptions;

public class TripsNotFoundException extends RuntimeException{
    public TripsNotFoundException(String message){
        super(message);
    }

}
