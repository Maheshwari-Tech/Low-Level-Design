package com.mycompany.app;

import com.mycompany.app.display.DisplayBoard;
import com.mycompany.app.spots.SpotType;
import com.mycompany.app.vehicles.Car;
import com.mycompany.app.vehicles.Vehicle;

public class Driver {

    public static void main(String[] args){

        ParkingLot lot = ParkingLot.getInstance();

        lot.addSpot(SpotType.TWO_WHEELER, 10);
        lot.addSpot(SpotType.FOUR_WHEELER, 20);

        DisplayBoard displayBoard = new DisplayBoard();
      //  displayBoard.addParkingLot(lot);
        //lot.addDisplayBoard(displayBoard);

        Entrance entry1 = new Entrance();
        Gate exit1 = new Exit();

        Gate entry2 = new Entrance();
        Gate exit2 = new Exit();

        Vehicle car = new Car("KA-01-MH-1234");
        entry1.process(car);


        exit1.process(car);

      //  displayBoard.showFreeSpots();


    }
}
