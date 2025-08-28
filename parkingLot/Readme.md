
## Design a parking lot 


* Parking lots  - 
* No of entry gates and exit gates -> Many , Many 
* Vehicle type Support - Bike, Car, Truck, Handicapped, 
* Parking spot type - 2_wheeler_SPOT, 4 wheeler spot, large vehicle spot, handicapped spot 
* Pricing ? 
  * Entry -- Ticket is issued. 
  * Exit -- Evaluate price based on ticket. 
* Ticket ? 
  * id, entry time, exit time, parkingSpotId, paymentId


* I know this is LLD, and we don't have db..
  * but I'm gonna still think in terms of Database to understand how to design this in real world. 
  * Then for the sake of LLD, I'll choose db as in memory db. 
  * Does that make sense ??


* Schema we need -- 
  * Vehicle - license no., metadata.. 
  * VehicleType - 2_WHEELER, 4_WHEELER 
  * ParkingSpot - id, type, status 
  * ParkingSpotType - Bike, Car 
  * EntryGate - id
  * ExitGate - id 

## What are we gonna store in db? 

* Tickets  - id, entryTime, exitTime, vehicle, price 
* RankSpotByGate - GateId, spotId, rank 
* 1: 1 relation on vehicle and spot -- 
    * VehicleSpot 
      * vehicleId, spotId

# Reason for doing it and not storing seperately on eiter of these? 
    * query can be on both side. which vehicle have which spot. 
    * vehicle is not something that is a attribute of spot. vehicle keeps on changing.
    * i guess, taking on spot table is also okay.. to save 2 phase commit.. 



## requirements - 

* R1: The parking lot must support a total capacity of up to 40,000 vehicles.

* R2: The parking lot must support multiple types of parking spots:
        Accessible (for individuals with disabilities)
        Compact
        Large
        Motorcycle

* R3: The parking lot should provide multiple entrance and exit points to support efficient traffic flow.

* R4: The system must support parking for four types of vehicles: cars, trucks, vans, and motorcycles.

* R5: A display board at each entrance and on every floor should show the current number of available parking spots for each parking spot type.

* R6: The system must not allow more vehicles to enter once the parking lot reaches its maximum capacity.

* R7: When the parking lot is fully occupied, a clear message should be shown at each entrance and on all parking lot display boards.

* R8: Customers must be issued a parking ticket at entry, which will be used to track parking time and calculate payment at exit.

* R9: Customers should be able to pay for parking at the automated exit panel.

* R10: The parking lot system must support configurable pricing rates based on vehicle type and/or parking spot type and different rates for different parking durations (e.g., first hour, subsequent hours).

* R11: Payments must be accepted via credit/debit card and cash at all payment points.



## Endpoints 

* CreateParkingLot(entryGates, exitGates)
* addParkingSpot((10, 2_WHEELER), (20, 4_WHEELER));
* rankSpotByGate()


* isSpaceAvailable(vehicleType)
* checkIn()
* checkOut() 

* display()









