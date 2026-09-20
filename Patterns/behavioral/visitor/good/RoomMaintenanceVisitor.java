package code.behavioral.visitor.good;

public class RoomMaintenanceVisitor implements RoomVisitor{
    @Override
    public void visit(SingleRoom element) {
        System.out.println("scheduled light maintenance for single room");
    }

    @Override
    public void visit(DeluxRoom element) {
        System.out.println("scheduled premium maintenance for deluxe room");
    }

    @Override
    public void visit(DoubleRoom element) {
        System.out.println("scheduled standard maintenance for double room");
    }
}
