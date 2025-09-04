package code.behavioral.mediator.OnlineAuctionSystem;

public interface AuctionMediator {
    void addBidder(Bidder bidder);
    void placeBid(Bidder bidder, int amount);
}
