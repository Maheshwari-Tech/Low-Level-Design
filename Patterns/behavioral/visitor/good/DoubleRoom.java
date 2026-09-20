package code.behavioral.visitor.good;

public class DoubleRoom implements RoomElement{

    private final int price;

    DoubleRoom(int price){
        this.price = price;
    }

    public int price() {
        return price;
    }

    @Override
    public void accept(RoomVisitor visitor) {
        visitor.visit(this);
    }
}
