package code.behavioral.visitor.good;

public class RoomPricingVisitor implements RoomVisitor{

    @Override
    public void visit(SingleRoom element) {
        System.out.println("single room price: " + element.price());
    }

    @Override
    public void visit(DeluxRoom element) {
        System.out.println("deluxe room price: " + element.price());
    }

    @Override
    public void visit(DoubleRoom element) {
        System.out.println("double room price: " + element.price());
    }
}
