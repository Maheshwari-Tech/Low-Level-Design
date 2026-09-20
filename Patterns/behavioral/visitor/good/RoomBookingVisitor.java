package code.behavioral.visitor.good;

public class RoomBookingVisitor implements RoomVisitor{
    @Override
    public void visit(SingleRoom element) {
        System.out.println("booked single room");
    }

    @Override
    public void visit(DeluxRoom element) {
        System.out.println("booked deluxe room");
    }

    @Override
    public void visit(DoubleRoom element) {
        System.out.println("booked double room");
    }
}
