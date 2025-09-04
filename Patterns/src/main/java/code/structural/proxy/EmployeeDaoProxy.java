package code.structural.proxy;

public class EmployeeDaoProxy implements EmployeeDao{

    private final EmployeeDao employeeDao;

    EmployeeDaoProxy(){
        this.employeeDao = new EmployeeDaoImpl();
    }
    @Override
    public void create(String client, Employee obj) {
        if(!client.equalsIgnoreCase("admin")){
            throw  new RuntimeException("access denied");
        }
        employeeDao.create(client, obj);
    }

    @Override
    public void delete(String client, int employeeId) {
        if(!client.equalsIgnoreCase("admin")){
            throw  new RuntimeException("access denied");
        }
        employeeDao.delete(client, employeeId);
    }

    @Override
    public EmployeeDao get(String client, int employeeId) {
        return employeeDao.get(client, employeeId);
    }
}
