package patterns.structural.decorator.pizzaExample.good;

public class Main {

    public static void main(String[] args) {

        BasePizza pizza = new ExtraCheese(new Margharita());
        System.out.println(pizza.cost());

        BasePizza pizza1 = new ExtraCheese(new MushroomTopping(new Margharita()));

        System.out.println(pizza1.cost());

    }
}
