# Proxy Design Pattern 

## Use case : 
* Access Restriction
* Caching 
* Pre-processing and post-processing - logging, publish event, etc. 

* marshaling and un-marshaling
* can have multiple layers of proxy
* client -> proxy1 -> proxy2 -> proxy3 -> realObject
* Anything that needs to be done at centralized. (ex: centralized logging)