## Blocking Queue | Bounded Buffer | Producer Consumer 

* Classical synchronization problem involving a limited size buffer which can have items added to it or
removed from it by different producer and consumer threads. 


### Problem Statement 

* A queue which blocks the caller of the enqueue method if there is no more capacity to add new item. 
* Blocks the caller of the dequeue method if there are no more items in the queue. 
* The queue notifies a blocked enqueuing thread when space become available and
* a blocked dequeuing thread when an item becomes available in the queue. 



## Solution - 


## Approach 1 : Synchronize - (Monitors)

1. Having a synchronize method for enque and deque. 
2. Each thread to notify all, important to save deadlock. 
3. say we have 2 producer, 1 producer and 1 consumer is waiting.. producer make it full and notify's the other producer.

## Approach 2 : Mutex 

