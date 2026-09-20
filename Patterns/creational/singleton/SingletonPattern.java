package patterns.creational.singleton;

/**
 * Singleton Pattern Example
 *
 * Intent: Ensure a class has only one instance and provide a global point of access to it.
 *
 * When to use:
 * - Exactly one instance of a class is needed
 * - Controlled access to a single object is necessary
 * - Lazy initialization is required
 *
 * Real-world examples:
 * - Database connection pool
 * - Configuration manager
 * - Logger
 * - Cache manager
 */
public class SingletonPattern {

    // Thread-safe Singleton with double-checked locking
    private static volatile SingletonPattern instance;

    private SingletonPattern() {
        // Private constructor prevents instantiation
    }

    public static SingletonPattern getInstance() {
        if (instance == null) {
            synchronized (SingletonPattern.class) {
                if (instance == null) {
                    instance = new SingletonPattern();
                }
            }
        }
        return instance;
    }

    public void doSomething() {
        System.out.println("Singleton instance is working!");
    }
}
