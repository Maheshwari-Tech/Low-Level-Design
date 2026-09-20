// VIOLATION: Dependency Inversion Principle
// High-level module depends on low-level module directly

class MySQLDatabase {
    public void connect() {
        System.out.println("Connecting to MySQL");
    }

    public void save(String data) {
        System.out.println("Saving to MySQL: " + data);
    }
}

class UserService {
    private MySQLDatabase database;

    public UserService() {
        this.database = new MySQLDatabase(); // Direct dependency
    }

    public void saveUser(String user) {
        database.connect();
        database.save(user);
    }
}

// Problems:
// 1. UserService is tightly coupled to MySQLDatabase
// 2. Can't easily switch to PostgreSQL or other databases
// 3. Hard to test without actual database
// 4. Changes to database affect UserService
