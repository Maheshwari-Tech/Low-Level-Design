// FIXED: Following Open-Closed Principle
// Classes are open for extension but closed for modification

public interface Shape {
    double calculateArea();
}

class Rectangle implements Shape {
    private double width, height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double calculateArea() {
        return width * height;
    }
}

class Circle implements Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

// New shapes can be added without modifying existing code
class Triangle implements Shape {
    private double base, height;

    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }

    @Override
    public double calculateArea() {
        return 0.5 * base * height;
    }
}

class AreaCalculator {
    // Now works with any Shape implementation
    public double calculateArea(Shape shape) {
        return shape.calculateArea();
    }

    // Can calculate total area of multiple shapes using streams
    public double calculateTotalArea(Shape... shapes) {
        return java.util.Arrays.stream(shapes)
            .mapToDouble(Shape::calculateArea)
            .sum();
    }

    public static void main(String[] args) {
        Shape[] shapes = {
                new Rectangle(4, 5),
                new Circle(3),
                new Triangle(6, 2)
        };
        double total = new AreaCalculator().calculateTotalArea(shapes);
        System.out.printf("total area: %.2f%n", total);
    }
}
