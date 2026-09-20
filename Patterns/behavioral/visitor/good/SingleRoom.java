package code.behavioral.visitor.good;

public class SingleRoom implements RoomElement{
    private final int price;

    SingleRoom(int price){
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
