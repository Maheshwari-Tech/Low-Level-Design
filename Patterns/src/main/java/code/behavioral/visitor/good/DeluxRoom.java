package code.behavioral.visitor.good;

public class DeluxRoom implements RoomElement{
    public int price;

    DeluxRoom(int price){
        this.price = price;
    }

    @Override
    public void accept(RoomVisitor visitor) {
        visitor.visit(this);
    }

}
