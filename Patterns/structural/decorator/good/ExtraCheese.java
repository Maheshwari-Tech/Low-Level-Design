package code.structural.decorator.good;

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
