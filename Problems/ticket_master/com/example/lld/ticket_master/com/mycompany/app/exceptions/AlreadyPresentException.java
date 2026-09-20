package com.mycompany.app.exceptions;

public class AlreadyPresentException extends RuntimeException {
    public AlreadyPresentException(String message) {
        super(message);
    }
}
