## Facade 

* Hide the system complexity from the client. 

* example -
* Complex system - payment, product, invoice, notification

* client -> order 
1. product
2. make payment 
3. generate invoice 
4. send notification 
5. new step .. 


Introduce a facade layer in between. 
client doesn't need to change code in case new step is added or 
in existing steps return type changes. 


### Relation with proxy - 
* proxy is 1:1, facade can be 1:1 or 1: many 
* intent of facade is to simplify, intent of proxy is to have additional control. 
* proxy must have common interface, facade may or may not have. 


## Relation with adapter - 
* adapter is used when client and original is incompatible, facade is compatible but simplify.
