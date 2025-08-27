S - single responsibility principle

O - open/closed principle

L - Liskov Substitution principle

I - Interface Segmented principle

D - Dependency Inversion principle

****

### S : SRP

A class should have only 1 reason to change.


```
@allargsconstructor
@getters 
@setters
class Marker{
	String name;
	String color;
	int year;
	int price;
}
```


```
@allargsconstructor
@getters
@setters
Class Invoice {
	private Marker marker; 
	private int quantity; 

	public int calculateTotal(){
		return marker.price * quantity;
	}

	public void printInvoice(){
		// prints the invoice
	}

	publuc void saveToDb(){
		// saves to DB
	}

}
```


Problem : This example have multiple reasons to change for the invoice class.

So we should be breaking this into multiple classes.
For example -


```
@allargsconstructor
@getters
@setters
class InvoicePrinter {
	Invoice invoice; 


	public void print(){
		// Prints the invoice 
	}
}
```

```
@allargsconstructor
@getters
@setters
class InvoiceDao{
	Invoice invoice; 
	public void saveToDB(){
	}
}
```

```
@allargsconstructor
@getters
@setters
Class Invoice {
	private Marker marker; 
	private int quantity; 

	public int calculateTotal(){
		return marker.price * quantity;
	}	
}
```


**Advantages** :
1. Easy to maintain and understand.

****

### O : Open closed principle

Open for extension but closed for modification


```
class InvoiceDao{
	Invoice invoice; 

	public void saveToDB(){
	}
	public void saveToFile(){
	}
}

```

Instead of this we should have -

```
interface InvoiceDao{
	public void save(Invoice invoice);
}


class DBInvoiceDao implements InvoiceDao {
	@Override
	public void save(Invoice invoice){
	}
}

class FileInvoiceDao implements InvoiceDao {
	@Override
	public void save(Invoice invoice){
	}
}

```


****

### L : Liskov Substitution principle


If class B is subtype of class A, then we should be able to replace he object of A with B without breaking the behavior of the program.

i.e. subclass should extend the capability of parent class not narrow it down.



```
interface Bike{
	void turnOnEngine();
	void accelerate();
}


class MotorCycle implements Bike {
	boolean isEngineOn; 
	int speed;

	

}
class Bicycle implements Bike {
	

}

class Car implements Bike {

}

```


Solution - use seperate interface

Vehicle, EnginedVehicle




*** 
### I: Interface Segmented principle



interfaces should be such, that client should not implement unnecessary methods they don't need.



```
interface RestauantEmployee{
	void wasDishes();
	void serverCustomers();
	void cookFood();
}
```




```
	interface WaiterInterface {
		void serverCustomers();
		void takeOrder();
	}


	interface ChefInterface {
		void cookFood();
		void decideMenu();
	}
```



*** 

### D:Dependency Inversion principle



class should depend on interfaces rather than concrete classes.


```
Imouse 
	- WiredMouse
	- BlueToothMouse
	
IKeyboard 
	- WiredKeyboard
	- BlueToothKeyboard

```

`
```
class MacBook {
	private final WiredKeyboard keyboard;
	Private final WiredMouse mouse;

	public MacBook(){
		keyboard = new WiredKeyboard();
		mouse = new WiredMouse();
	}
}

```
````
class MacBook {
	private final Keyboard keyboard;
	Private final Mouse mouse;

	public MacBook(Keyboard k, Mouse m){
		keyboard = k;
		mouse = m;
	}
}
`