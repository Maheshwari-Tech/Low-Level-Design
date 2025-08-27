package code.exceptions;

public class RidersNotFoundException extends RuntimeException{

    public RidersNotFoundException(String message){
        super(message);
    }
}
