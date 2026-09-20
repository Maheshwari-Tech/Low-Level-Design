package patterns.structural.decorator.pizzaExample.good;

public class ExtraCheese extends ToppingDecorator {
    BasePizza basePizza;
    ExtraCheese(BasePizza basePizza) {
        this.basePizza = basePizza;
    }

    @Override
    int cost() {
        return basePizza.cost() + 21;
    }
}
