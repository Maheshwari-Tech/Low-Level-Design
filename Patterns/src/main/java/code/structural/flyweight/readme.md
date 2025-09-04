## Flyweight design pattern 

* Structural design pattern
* Reduce memory usage by sharing data between multiple objects.

* when memory is limited. 
* When object shared the data. 
  * Intrinsic data - shared among objects and remain same once defined one value. 
  * Extrinsic data - changes based on client input and differs from one object to others.
* creation of object is expensive. 


* from object, remove all the extrinsic data and keep intrinsic data (this object called flyweight object.)
* Flyweight class can be immutable. 
* Extrinsic data cam be passed to the flyweight class in method parameter. 
* Once the flyweight object is created, it is cached and reused whenever required. 

