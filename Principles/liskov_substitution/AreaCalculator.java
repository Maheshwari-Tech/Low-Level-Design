// VIOLATION: Liskov Substitution Principle
// Square cannot be substituted for Rectangle without breaking behavior

class Rectangle {
    protected int width, height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return width * height;
    }
}

// This breaks LSP - Square has different behavior than Rectangle
class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        super.setHeight(width); // Breaking change!
    }

    @Override
    public void setHeight(int height) {
        super.setWidth(height); // Breaking change!
        super.setHeight(height);
    }
}

public class AreaCalculator {
    public static void main(String[] args) {
        Rectangle rect = new Rectangle();
        rect.setWidth(5);
        rect.setHeight(10);
        System.out.println("Rectangle area: " + rect.getArea()); // 50

        // This should work the same way, but it doesn't!
        Rectangle square = new Square();
        square.setWidth(5);
        square.setHeight(10);
        System.out.println("Square area: " + square.getArea()); // 100 (unexpected!)
    }
}
