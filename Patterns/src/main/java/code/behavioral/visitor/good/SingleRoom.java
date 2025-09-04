package code.behavioral.visitor.good;

public class SingleRoom implements RoomElement{
    public int price;

    SingleRoom(int price){
        this.price = price;
    }

    @Override
    public void accept(RoomVisitor visitor) {
        visitor.visit(this);
    }
}
