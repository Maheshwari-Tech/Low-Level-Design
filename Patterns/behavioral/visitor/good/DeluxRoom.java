package code.behavioral.visitor.good;

public class DeluxRoom implements RoomElement{
    private final int price;

    DeluxRoom(int price){
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
