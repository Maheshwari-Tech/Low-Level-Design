package code.behavioral.visitor.good;

public interface RoomElement {
    void accept(RoomVisitor visitor);
}
