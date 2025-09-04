package code.structural.proxy;

public class Demo {
    public static void main(String[] args) {

        try {
            EmployeeDao employeeDao = new EmployeeDaoProxy();
            employeeDao.get("user", 1);
            employeeDao.delete("admin", 2);
            employeeDao.create("admin", new Employee());

            employeeDao.delete("user", 2);
            employeeDao.create("user", new Employee());
        } catch (RuntimeException e) {
           System.out.println(e.getMessage());
        }

    }
}
