
# Observer Design Pattern 


### Problem  statement 

* amazon site search product 
* out of stock 
* Notify me.
* Implement Notify me 



Observable         Observer 

states : S1, S2, S3

Whenever there is a state change in observable, 
it needs to send update observers.


```
IObservable

List<IObserver> observers;

add(IObserver observer);
remove(IObserver observer);

notify();  
setData();
```


```
IObserver

update()
```


```
Observable
{
notify(){
    for(IObserver observer: objects){
            observer.update(__); 
    }   
}

setData(){
    notify();
}
}

```


### 2. 
Weather Station 

current Temp 

TV Display 
Mobile Display 

```
IWSObservable {    
    add(IDisplayObserver);
    remove(IDisplayObserver);
    
    notify();
    
    setTemp();
    getTemp();
        
}

WSObservableImpl {
    List<IDisplayObserver> displays; 
    int temp; 
    
    void notify(){
        for(IDisplayObserver observer: displays){
            observer.update();
        }   
    }
    
    setTemp(int temp){
        if(temp ! = this.temp){
            this.temp = temp;
            notify();
         }
    }
}

IDisplayObserver {
    update();
}

MobileDisplayObserver {
    IWSObservable observable;
    
    MobileDisplayObserver(IWSObservable observable){
        this.observable = observable;
    }
    update(){
        observable.getData();
    }
}

WeatherDisplayObserver {
    IWSObservable observable;
    
    WeatherDisplayObserver(IWSObservable observable){
        this.observable = observable;
    }
    update(){
        observable.getData();
    }
}

```


