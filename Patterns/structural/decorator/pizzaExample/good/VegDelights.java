package patterns.structural.decorator.pizzaExample.good;

public class VegDelights extends BasePizza{
    @Override
    int cost() {
        return 400;
    }
}
