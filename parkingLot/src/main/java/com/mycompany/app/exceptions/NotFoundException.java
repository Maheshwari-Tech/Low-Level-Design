package com.mycompany.app.exceptions;

public class NotFoundException extends RuntimeException{
    String msg;
    String code;

    NotFoundException(String msg){
        this.msg = msg;
        this.code = "500";
    }

    NotFoundException(String msg, String code){
        this.msg = msg;
        this.code = code;
    }
}
