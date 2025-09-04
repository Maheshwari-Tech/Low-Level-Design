package code.creational.factory;

public class ShapeFactory {

    Shape getShape(String type){
        switch (type){
            case "circle" -> {
                return new Circle();
            }
            case "rectangle" -> {
                return new Rectangle();
            }
            default -> {
                return null;
            }
        }
    }
}
