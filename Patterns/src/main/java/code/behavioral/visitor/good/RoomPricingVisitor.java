package code.behavioral.visitor.good;

public class RoomPricingVisitor implements RoomVisitor{

    @Override
    public void visit(SingleRoom element) {
        System.out.println(" ");
    }

    @Override
    public void visit(DeluxRoom element) {

    }

    @Override
    public void visit(DoubleRoom element) {

    }


}
