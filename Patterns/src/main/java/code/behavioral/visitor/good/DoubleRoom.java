package code.behavioral.visitor.good;

public class DoubleRoom implements RoomElement{

    public int price;

    DoubleRoom(int price){
        this.price = price;
    }

    @Override
    public void accept(RoomVisitor visitor) {
        visitor.visit(this);
    }
}
