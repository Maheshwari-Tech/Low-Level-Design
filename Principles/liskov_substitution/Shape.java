// FIXED: Following Liskov Substitution Principle
// Use composition instead of inheritance for different behaviors

interface Shape {
    double getArea();
}

class Rectangle implements Shape {
    protected double width, height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public double getArea() {
        return width * height;
    }
}

class Square implements Shape {
    private double side;

    public Square(double side) {
        this.side = side;
    }

    public void setSide(double side) {
        this.side = side;
    }

    @Override
    public double getArea() {
        return side * side;
    }
}

// Now both can be used interchangeably without breaking behavior
class AreaCalculator {
    public static void main(String[] args) {
        Shape rect = new Rectangle(5, 10);
        Shape square = new Square(5);

        System.out.println("Rectangle area: " + rect.getArea()); // 50
        System.out.println("Square area: " + square.getArea());   // 25

        // Both work as expected - LSP satisfied!
        calculateAndPrintArea(rect);
        calculateAndPrintArea(square);
    }

    private static void calculateAndPrintArea(Shape shape) {
        System.out.println("Shape area: " + shape.getArea());
    }
}
