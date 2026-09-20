package code.behavioral.visitor.good;

public interface RoomVisitor {
    void visit(SingleRoom element);
    void visit(DeluxRoom element);
    void visit(DoubleRoom element);

}
