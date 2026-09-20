package code.behavioral.mediator.OnlineAuctionSystem;

public interface Bidder {
    void placeBid(int amount);
    void receiveNotification(int amount);
    void setMediator(AuctionMediator auctionMediator);
    String getName();
}
