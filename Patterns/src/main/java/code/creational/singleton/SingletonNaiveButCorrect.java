package code.creational.singleton;

public class SingletonNaiveButCorrect {

    private static final SingletonNaiveButCorrect instance = new SingletonNaiveButCorrect();

    private SingletonNaiveButCorrect(){

    }

    public static SingletonNaiveButCorrect getInstance() {
        return instance;
    }

    public void method(){
        System.out.println("executing method");
    }
}
