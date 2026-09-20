package code.behavioral.mediator.OnlineAuctionSystem;


import java.util.ArrayList;
import java.util.List;

public class Auction implements AuctionMediator{
    private final List<Bidder> bidders = new ArrayList<>();
    private int highestBid;

    @Override
    public void addBidder(Bidder bidder) {
        bidders.add(bidder);
    }

    @Override
    public void placeBid(Bidder bidder, int amount) {
        if (amount <= highestBid) {
            throw new IllegalArgumentException("bid must exceed " + highestBid);
        }
        highestBid = amount;
        System.out.printf("%s placed the new highest bid: %d%n", bidder.getName(), amount);

        for(Bidder other : bidders){
            if(other != bidder){
                other.receiveNotification(amount);
            }
        }
    }
}
