package code.behavioral.mediator.OnlineAuctionSystem;

public class Main {
    public static void main(String[] args) {
        AuctionMediator auction = new Auction();
        Bidder bidder = new BidderImpl("a");
        Bidder bidder2 = new BidderImpl("b");
        Bidder bidder3 = new BidderImpl("c");

        bidder.setMediator(auction);
        bidder2.setMediator(auction);
        bidder3.setMediator(auction);

        bidder2.placeBid(10);
        bidder.placeBid(100);
    }
}
