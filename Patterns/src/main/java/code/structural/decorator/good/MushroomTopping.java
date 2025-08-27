package code.structural.decorator.good;

public class MushroomTopping extends ToppingDecorator{

    BasePizza basePizza;

    MushroomTopping(BasePizza basePizza) {
       this.basePizza = basePizza;
    }

    @Override
    int cost() {
       return basePizza.cost() + 40;
    }
}
