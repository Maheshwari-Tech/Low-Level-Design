// VIOLATION: Open-Closed Principle
// This class needs to be modified every time a new shape is added

public class AreaCalculator {
    public double calculateArea(Object shape) {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            return rect.width * rect.height;
        } else if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            return Math.PI * circle.radius * circle.radius;
        }
        // Need to add more conditions for new shapes...
        return 0;
    }
}

class Rectangle {
    public double width, height;
}

class Circle {
    public double radius;
}
