package code.structural.proxy;

public class Demo {
    public static void main(String[] args) {
        EmployeeDao employeeDao = new EmployeeDaoProxy();
        employeeDao.get("user", 1);
        employeeDao.delete("admin", 2);
        employeeDao.create("admin", new Employee());

        attempt(() -> employeeDao.delete("user", 2));
        attempt(() -> employeeDao.create("user", new Employee()));
    }

    private static void attempt(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
