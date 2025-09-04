package code.structural.proxy;

public interface EmployeeDao {
    void create(String client, Employee employee);
    void delete(String client, int employeeId);
    EmployeeDao get(String client, int employeeId);
}
