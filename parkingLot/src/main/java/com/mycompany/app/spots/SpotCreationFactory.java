package com.mycompany.app.spots;

public class SpotCreationFactory {

    public Spot createSpot(SpotType spotType){
        return switch (spotType) {
            case TWO_WHEELER -> new TwoWheelerSpot();
            case FOUR_WHEELER -> null;
        };
    }
}
