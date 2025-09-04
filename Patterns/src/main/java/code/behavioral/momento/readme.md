## Momento design pattern 

* Also known as snapshot pattern. 
* Doesn't expose object's internal state. 


* Major component in "momento pattern"
* Originator  - it represents the object.
* Momento - it represents an object which holds the state of the originator.
* Caretaker - List of momento


Momento 
State state 
setState(State state)
State getState()


Originator
State state 
Momento createMomento()
restoreMomento(Momento m)


Caretaker 
List<Momento> history 
addMomento(Memento obj)
Memento undo()
