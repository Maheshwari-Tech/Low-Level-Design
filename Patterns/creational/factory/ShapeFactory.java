package code.creational.factory;

import java.util.Locale;
import java.util.Objects;

public class ShapeFactory {

    public Shape getShape(String type) {
        switch (Objects.requireNonNull(type, "type").toLowerCase(Locale.ROOT)) {
            case "circle" -> {
                return new Circle();
            }
            case "rectangle" -> {
                return new Rectangle();
            }
            default -> {
                throw new IllegalArgumentException("unknown shape: " + type);
            }
        }
    }
}
