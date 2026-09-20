package code.creational.singleton;

import patterns.creational.singleton.SingletonPattern;

public final class SingletonDemo {
    private SingletonDemo() {
    }

    public static void main(String[] args) {
        SingletonNaiveButCorrect first = SingletonNaiveButCorrect.getInstance();
        SingletonNaiveButCorrect second = SingletonNaiveButCorrect.getInstance();
        if (first != second) {
            throw new AssertionError("singleton returned different instances");
        }
        first.method();
        System.out.println("same eager instance: true");

        SingletonPattern lazyFirst = SingletonPattern.getInstance();
        SingletonPattern lazySecond = SingletonPattern.getInstance();
        if (lazyFirst != lazySecond) {
            throw new AssertionError("lazy singleton returned different instances");
        }
        lazyFirst.doSomething();
        System.out.println("same lazy instance: true");
    }
}
