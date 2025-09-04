package code.structural.proxy;

public class EmployeeDaoImpl  implements EmployeeDao{
    @Override
    public void create(String client, Employee employee) {
        System.out.println("creating");
    }

    @Override
    public void delete(String client, int employeeId) {
        System.out.println("deleting");

    }

    @Override
    public EmployeeDao get(String client, int employeeId) {
        System.out.println("fetching");
        return new EmployeeDaoImpl();
    }
}
