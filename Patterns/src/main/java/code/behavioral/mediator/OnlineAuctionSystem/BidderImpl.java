package code.behavioral.mediator.OnlineAuctionSystem;

public class BidderImpl implements Bidder {
    private final String name;
    private AuctionMediator auctionMediator;

    public BidderImpl(String name){
        this.name = name;

    }
    @Override
    public void placeBid(int amount) {
        auctionMediator.placeBid(this, amount);
    }

    @Override
    public void receiveNotification(int amount) {
        System.out.println(name + " received notification of " + amount);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void setMediator(AuctionMediator auctionMediator) {
        auctionMediator.addBidder(this);
        this.auctionMediator = auctionMediator;
    }
}
