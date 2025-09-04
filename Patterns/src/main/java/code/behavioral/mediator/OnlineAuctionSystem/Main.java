package code.behavioral.mediator.OnlineAuctionSystem;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello and welcome!");

        for (int i = 1; i <= 5; i++) {
            System.out.println("i = " + i);
        }

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