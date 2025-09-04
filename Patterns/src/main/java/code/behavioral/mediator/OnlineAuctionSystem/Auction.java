package code.behavioral.mediator.OnlineAuctionSystem;


import java.util.ArrayList;
import java.util.List;

public class Auction implements AuctionMediator{
    List<Bidder> bidders = new ArrayList<>();

    @Override
    public void addBidder(Bidder bidder) {
        bidders.add(bidder);
    }

    @Override
    public void placeBid(Bidder bidder, int amount) {

        for(Bidder other : bidders){
            if(other != bidder){
                other.receiveNotification(amount);
            }
        }
    }
}
