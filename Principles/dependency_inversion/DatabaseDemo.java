// FIXED: Following Dependency Inversion Principle
// Both high-level and low-level modules depend on abstractions

interface Database {
    void connect();
    void save(String data);
}

class MySQLDatabase implements Database {
    @Override
    public void connect() {
        System.out.println("Connecting to MySQL");
    }

    @Override
    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
}

class PostgreSQLDatabase implements Database {
    @Override
    public void connect() {
        System.out.println("Connecting to PostgreSQL");
    }

    @Override
    public void save(String data) {
        System.out.println("Saving to PostgreSQL: " + data);
    }
}

class UserService {
    private Database database;

    // Dependency injection - abstraction, not concrete class
    public UserService(Database database) {
        this.database = database;
    }

    public void saveUser(String user) {
        database.connect();
        database.save(user);
    }
}

public class DatabaseDemo {
    public static void main(String[] args) {
        // Can easily switch between database implementations
        Database mysql = new MySQLDatabase();
        Database postgres = new PostgreSQLDatabase();

        UserService service1 = new UserService(mysql);
        UserService service2 = new UserService(postgres);

        service1.saveUser("John");
        service2.saveUser("Jane");

        // Benefits:
        // 1. Loose coupling between UserService and database
        // 2. Easy to switch databases
        // 3. Easy to test with mock databases
        // 4. Both depend on abstractions, not concretions
    }
}
