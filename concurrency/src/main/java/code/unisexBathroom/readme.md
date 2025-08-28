## Problem statement 

* A Bathroom is being designed for the use of both males and females in an office
but requires the following constraints to be maintained. 

* There cannot be men and women in the bathroom at the same time. 
* There should never be more than three employees in the bathroom simultaneously. 

* The solution should avoid deadlocks. For now, though, don't worry about starvation. 

## Approach  

1. We need a class uniSexBathroom and 2 seperate method to mimic maleUseBathRoom and femaleUseBathRoom.
2. A method useBathRoom - just sleep for some time. 
3. a variable to store inUseBy. 
4. for maleUseBathRoom - while bathroom is in use by female wait.
5. when wait gets over -> update inUseBy. 
6. (5) needs to be sync so does 4th then. 


## How can we solve for starvation:
A.
1. queue based on timestamp.
2. don't give permit if time is > say 30 seconds of waiting for other party.

B. fairness -- first come, first turn. although not optimal. ()



