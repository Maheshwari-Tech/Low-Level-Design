# kumaransg/LLD variation index

This index maps the meaningful content in the clean [kumaransg/LLD snapshot](./kumaransg-LLD/) to this workspace's canonical question families. It is pinned to upstream commit [`1698cc6f993a5014d4370b5e0db9f64d322e2400`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400).

Labels are literal:

- **variation** means a materially different implementation or scope and should remain discoverable.
- **exact duplicate** means source files match another path; retain one canonical attachment and record the alias.
- **requirements only**, **diagram only**, or **fragment** means it is not a complete runnable solution.
- **outbound-link only** means the clone contains a prompt name and external URL, not the linked solution.

The aggregate repository has no root license. Paths are indexed for provenance; only the four explicitly licensed subtrees listed in [README.md](./README.md#license-boundaries) can be redistributed under their stated terms.

## airline_management_system

- [FlightTrackingSystem](./kumaransg-LLD/Low_level_Design_Problems/FlightTrackingSystem/) — Java, plain source; flight lookup by departure, arrival, and route; focused variation of the broader airline domain.
- [PlaneReservation.java](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/ds/graph/dfs/PlaneReservation.java) — Java/Maven collection; single-file reservation/search fragment.

## atm_system

- [ATMMachine](./kumaransg-LLD/Low_level_Design_Problems/ATMMachine/) — Java, plain source; ATM devices, accounts, deposits, withdrawals, and transaction hierarchy.
- [oops/atm](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/atm/) — Java/Maven collection; alternate object-model-first ATM sketch.

## cab_booking_service

- [DriverAndRiderApplication](<./kumaransg-LLD/Ride Sharing /DriverAndRiderApplication/>) — Java/Eclipse; driver eligibility and rider matching with tests.
- [Ride-Sharing Application](<./kumaransg-LLD/Ride Sharing /Ride-Sharing Application/>) — Java/Maven; offered rides, selection strategies, lifecycle, and statistics.
- [RideShare_MachineCoding_Sample](<./kumaransg-LLD/Ride Sharing /RideShare_MachineCoding_Sample/>) — Java, plain source; MIT-licensed ride matching variation.
- [RideSharing2](<./kumaransg-LLD/Ride Sharing /RideSharing2/>) — Java, plain source; registration, booking, and proximity-based matching variation.
- [cab_booking_system](<./kumaransg-LLD/Ride Sharing /cab_booking_system/>) — Java, plain source; cab availability and trip lifecycle.
- [miniuber](<./kumaransg-LLD/Ride Sharing /miniuber/>) — Java, plain source; compact five-file Uber model.
- [ride-sharing-low-level-design](<./kumaransg-LLD/Ride Sharing /ride-sharing-low-level-design/>) — Java/Maven; MIT-licensed basic ride-sharing model with class diagram and PDF.
- [ridesharing](<./kumaransg-LLD/Ride Sharing /ridesharing/>) — Java, plain source; controllers, selection policies, services, and exceptions.
- [ridesharing1](<./kumaransg-LLD/Ride Sharing /ridesharing1/>) — Java/IntelliJ; one authored source file plus generated output; incomplete fragment.
- [uber](<./kumaransg-LLD/Ride Sharing /uber/>) — Java/Gradle; larger domain and service implementation.
- [cabBooking](./kumaransg-LLD/Low_level_Problem_set_2/cabBooking/) — Java, plain source; DAO-oriented cab-booking implementation.
- [cabBooking2](./kumaransg-LLD/Low_level_Problem_set_2/cabBooking2/) — Java, plain source; second DAO/model implementation.
- [FlipkartMachineCoding](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/FlipkartMachineCoding/) — Java/Maven; customer/driver eligibility strategy rather than full trip management.
- [DriverAndRiderApplication.zip](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/DriverAndRiderApplication.zip) — archive; exact source duplicate of the top-level `DriverAndRiderApplication`; exclude from curated copies.
- [Uber-Ola Design](<./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Uber-Ola Design/>) — empty placeholder; no solution.

## cache

- [DistributedCache](./kumaransg-LLD/Low_level_Design_Problems/DistributedCache/) — Java, plain source; separate consistent-hashing and LRU-cache sketches.
- [LowLevelCacheDesign/lld-cache](./kumaransg-LLD/Low_level_Design_Problems/LowLevelCacheDesign/lld-cache/) — Java/Maven; storage and eviction abstractions with tests.
- [SystemDesign/LowLevelCacheDesign](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LowLevelCacheDesign/) — Java, plain source; factory plus LRU/storage abstractions and tests.
- [cache-low-level-design](./kumaransg-LLD/Low_level_Design_Problems/cache-low-level-design/) — Java/Maven; MIT-licensed cache with class diagram.
- [Problem set cache](./kumaransg-LLD/Low_level_Problem_set_2/cache/) — Java, plain source; policy and storage interfaces.

## chess_game

- [chessGame](./kumaransg-LLD/Low_level_Design_Problems/chessGame/) — Java, plain source; board and move model.
- [chessGame_new](./kumaransg-LLD/Low_level_Design_Problems/chessGame_new/) — Java, plain source; alternate board and move model.
- [designchess](./kumaransg-LLD/Low_level_Design_Problems/designchess/) — Java, plain source; controller/model implementation.
- [Low-Level-Design-1/designchess](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designchess/) — exact duplicate of root `designchess` except Finder metadata.
- [mychess.com](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/mychess.com/) — Java, plain source; compact chess variation with README.
- [lld/chess](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/chess/) — Java/Maven collection; piece and board model.
- [oops/chessv1](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/chessv1/) — Java/Maven collection; account, game, move, and piece model.
- [systemdesign/chess](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/chess/) — Java/Maven collection; second piece-and-board model.
- [Problem set chess](./kumaransg-LLD/Low_level_Problem_set_2/chess/) — Java, plain source; constants, DAO, and model implementation.

## coupon_promotion_engine

- [Coupon](./kumaransg-LLD/Low_level_Design_Problems/Coupon/) — Java, plain source; heterogeneous items and composable cart coupons.
- [systemdesign/shoppingcart](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/shoppingcart/) — Java/Maven collection; minimal offer-product variation; fragment.

## cricinfo

- [Cricket Match Dashboard](<./kumaransg-LLD/Cricket Match Dashboard/>) — Java/IntelliJ; full ball-by-ball scorecard prompt and implementation.
- [cricket-scorecard](./kumaransg-LLD/Low_level_Design_Problems/cricket-scorecard/) — Java/Maven; separate scorecard model and service variation.

## custom_hash_map

- [DesignDataStructures/HashMap](./kumaransg-LLD/Low_level_Design_Problems/DesignDataStructures/HashMap/) — Java, plain source; custom map implementation.
- [Leetcode/G4G/DesignDataStructures/HashMap](./kumaransg-LLD/Low_level_Design_Problems/Leetcode/G4G/src/DesignDataStructures/HashMap/) — exact duplicate of the preceding implementation.

## e_commerce

- [Amazon](./kumaransg-LLD/Low_level_Design_Problems/Amazon/) — diagram only; Amazon/Flipkart system sketch.
- [amazon_new](./kumaransg-LLD/Low_level_Design_Problems/amazon_new/) — Java, plain source; accounts, catalog, cart, order, shipment, and payment model.
- [oops/amazon](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/) — Java/Maven collection; alternate catalog/cart/order/shipment model.
- [Problem set amazon](./kumaransg-LLD/Low_level_Problem_set_2/amazon/) — Java, plain source; constants, DAO, and model implementation.

## elevator_system

- [LLD-Practice/Elevator](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Elevator/) — Java/Maven; elevator practice implementation.
- [SystemDesign/ElevatorDesign](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ElevatorDesign/) — Java, plain source; controller and event-listener approach.
- [SystemDesign/ElevatorDesign2](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ElevatorDesign2/) — Java, plain source; alternate request and strategy model.
- [Problem set elevator](./kumaransg-LLD/Low_level_Problem_set_2/elevator/) — Java, plain source; compact four-file implementation.
- [thegranths/SystemDesign/Elevator](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/SystemDesign/Elevator/) — Java/Maven; another compact implementation.

## file_system

- [FileAndDirectorySystem](./kumaransg-LLD/Low_level_Design_Problems/FileAndDirectorySystem/) — Java, plain source; entries plus name, extension, and size search strategies.
- [System-design/FileSystem](./kumaransg-LLD/Low_level_Design_Problems/System-design/FileSystem/) — Java, plain source; file/folder and search sketch.
- [systemdesign/directorystructure](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/directorystructure/) — Java/Maven collection; entity/file/directory variation.
- [low-level-design/fs](./kumaransg-LLD/Low_level_Design_Problems/low-level-design/fs/) — Java, plain source; alternate file-system implementation.

## food_delivery_service

- [FoodKart](./kumaransg-LLD/FoodKart/) — Java/IntelliJ; full restaurant discovery, rating, stock, and ordering prompt and solution.
- [lld-food-delivery-zomato-swiggy](./kumaransg-LLD/Low_level_Design_Problems/lld-food-delivery-zomato-swiggy/) — Java/Maven; broad customer, restaurant, menu, order, delivery, and payment model.
- [food-ordering-system](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/food-ordering-system/) — Java, plain source; compact ordering variation.
- [SwiggyDeliveryBoy](./kumaransg-LLD/Low_level_Design_Problems/SwiggyDeliveryBoy/) — Java/Maven; route guidance from coordinates; focused fragment.
- [swiggy-delivery](./kumaransg-LLD/Low_level_Design_Problems/swiggy-delivery/) — Java/Maven; delivery assignment variation.
- [swiggyInterview1](./kumaransg-LLD/Low_level_Design_Problems/swiggyInterview1/) — Java/Maven/Spring; delivery-executive scoring and assignment strategies.

## hotel_management

- [HotelManagmentSystem](./kumaransg-LLD/Low_level_Design_Problems/HotelManagmentSystem/) — Java, plain source; booking, services, payment, invoice, notification, and diagram.
- [lld/hotelbooking](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/hotelbooking/) — Java/Maven collection; database-object model for hotels, rooms, facilities, and reservations.

## library_management_system

- [Library Management System](<./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Library Management System/>) — Java, plain source; requirements plus lending and fine services.
- [lld/library](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/library/) — Java/Maven collection; separate database and domain models.
- [SystemDesign/LibraryManagementSystem](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LibraryManagementSystem/) — Java, plain source; catalog, copies, lending, fines, and reservations.

## logging

- [designLogger](./kumaransg-LLD/Low_level_Design_Problems/designLogger/) — Java, plain source; API, configuration, model, and output strategy.
- [Low-Level-Design-1/designLogger](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designLogger/) — exact duplicate of root `designLogger`.

## multi_level_cache

- [Multi-Level-Cache](./kumaransg-LLD/Low_level_Design_Problems/Multi-Level-Cache/) — Java, plain source; N-level read/write promotion and statistics prompt.
- [multilevelcache](./kumaransg-LLD/Low_level_Design_Problems/multilevelcache/) — Java/Maven; separate multi-level cache implementation and prompt.
- [Low-Level-Design-1/multilevelcache](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/multilevelcache/) — exact duplicate of root `multilevelcache`.

## music_streaming_service

- [SystemDesign/JukeBox](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/JukeBox/) — Java, plain source; CD player, playlist, song, artist, and user device-oriented variation.

## online_bookstore

- [System-design/online_book_reader_system](./kumaransg-LLD/Low_level_Design_Problems/System-design/online_book_reader_system/) — Java, plain source; reader-oriented model, not retail checkout; preserve as a named variation.
- [SystemDesign/OnlineBookReaderSystem](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem/) — Java, single-file nested design; reader fragment.
- [SystemDesign/OnlineBookReaderSystem2](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/OnlineBookReaderSystem2/) — Java, single-file alternate reader design.

## order_processing_system

- [Order-Booking-System](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/) — Java/Eclipse; add, purchase, return, blacklist, category, and bestseller workflow.
- [SwiggyInterview](./kumaransg-LLD/Low_level_Design_Problems/SwiggyInterview/) — Java/Maven; order-state history and SLA-monitoring jobs with alert publishing.
- [SystemDesign/LogisticsDesign](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LogisticsDesign/) — Java, plain source; order status, priority, payment, vehicle, and logistics model.
- [Order-Booking-System.zip](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System.zip) — archive; exact duplicate of the extracted source tree; exclude from curated copies.

## parking_lot

- [ParkingLot1](./kumaransg-LLD/Low_level_Design_Problems/ParkingLot1/) — Java, plain source; domain/services approach with requirements.
- [SystemDesign/ParkingLot](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ParkingLot/) — Java, plain source; compact levels/spots/vehicle-size model.
- [SystemDesign/ParkingLot2](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ParkingLot2/) — Java, plain source; larger account, floor, panel, ticket, and typed-spot model.
- [multilevelparkinglot](./kumaransg-LLD/Low_level_Design_Problems/multilevelparkinglot/) — Java/Maven; multi-floor implementation with tests.
- [Low-Level-Design-1/multilevelparkinglot](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/multilevelparkinglot/) — exact duplicate of root `multilevelparkinglot` except Finder metadata.
- [parking-lot-lld-oop-ood](./kumaransg-LLD/Low_level_Design_Problems/parking-lot-lld-oop-ood/) — Java/Gradle; interview expectations, DDD discussion, and implementation.
- [parkingLot_new](./kumaransg-LLD/Low_level_Design_Problems/parkingLot_new/) — Java, plain source; alternate object model.
- [parkinglot](./kumaransg-LLD/Low_level_Design_Problems/parkinglot/) — Java/Gradle; Apache-2.0 licensed implementation.
- [parkinglot_services](./kumaransg-LLD/Low_level_Design_Problems/parkinglot_services/) — Java, plain source; controller/model/service approach.
- [Low-Level-Design-1/parkinglot_services](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/parkinglot_services/) — exact duplicate of root `parkinglot_services` except Finder metadata.
- [oops/parkinglot](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/parkinglot/) — Java/Maven collection; broad account, panel, floor, spot, ticket, and vehicle model.
- [Problem set parkingLot](./kumaransg-LLD/Low_level_Problem_set_2/parkingLot/) — Java, plain source; first DAO/service implementation.
- [Problem set parkingLot2](./kumaransg-LLD/Low_level_Problem_set_2/parkingLot2/) — Java, plain source; second DAO/model implementation.
- [thegranths/ParkingLot](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/SystemDesign/ParkingLot/) — Java/Maven; another model-focused variation.
- [Parking Lot Design requirements](<./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Parking Lot Design/Requirements.txt>) — requirements only; no implementation in that directory.

## product_catalog_service

- [amazon_new](./kumaransg-LLD/Low_level_Design_Problems/amazon_new/) — Java, plain source; catalog, categories, products, search, reviews, and item modeling inside an e-commerce solution.
- [oops/amazon](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/amazon/) — Java/Maven collection; alternate catalog/search/product model.
- [Order-Booking catalog](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/Order-Booking-System/src/com/flipkart/catalog/) — Java/Eclipse; small category/product catalog fragment.

## pub_sub_system

- [designpubsub](./kumaransg-LLD/Low_level_Design_Problems/designpubsub/) — Java, plain source; handlers, models, and public interfaces.
- [Low-Level-Design-1/designpubsub](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designpubsub/) — exact duplicate of root `designpubsub` except Finder metadata.
- [low-level-design-messaging-queue-pub-sub](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-messaging-queue-pub-sub/) — Java, plain source; topic, subscriber, offset, and queue APIs with problem statement.
- [message-broker](./kumaransg-LLD/Low_level_Design_Problems/message-broker/) — Java/Maven; broker, publisher, subscriber, and topic model.
- [pub-sub Phonepe](<./kumaransg-LLD/Low_level_Design_Problems/pub-sub Phonepe/>) — Java/IntelliJ; PhonePe prompt and implementation.
- [Problem set kafka](./kumaransg-LLD/Low_level_Problem_set_2/kafka/) — Java, plain source; Kafka-like handlers, models, and interfaces.
- [multiThreadedMessageQueue](./kumaransg-LLD/Low_level_Problem_set_2/multiThreadedMessageQueue/) — Java, plain source; concurrent queue variation.

## rate_limiter

- [RateLimiter](./kumaransg-LLD/Low_level_Design_Problems/RateLimiter/) — Java, plain source; compact limiter implementation.
- [TokenBucket.java](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/ratelimiter/TokenBucket.java) — Java/Maven collection; single-file token-bucket variation.

## restaurant_management_system

- [FoodKart](./kumaransg-LLD/FoodKart/) — Java/IntelliJ; restaurant registration, serviceability, inventory, rating, sorting, and orders.
- [food-ordering-system](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/food-ordering-system/) — Java, plain source; restaurant/menu/order variation.

## shopping_cart_with_expiration

- [Coupon cart](./kumaransg-LLD/Low_level_Design_Problems/Coupon/src/io/abhi/cartdesign/) — Java, plain source; cart composition and coupons; no expiration behavior.
- [systemdesign/shoppingcart](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/shoppingcart/) — Java/Maven collection; offer-product fragment; no expiration behavior.

## snake_ladder

- [SnakeLadderGame](./kumaransg-LLD/Low_level_Design_Problems/SnakeLadderGame/) — Java, plain source; compact game implementation.
- [SnakesAndLadder](./kumaransg-LLD/Low_level_Design_Problems/SnakesAndLadder/) — Java, plain source; prompt, sample input, diagram, and packaged output.
- [snakeandladder](./kumaransg-LLD/Low_level_Design_Problems/snakeandladder/) — Java, plain source; model/service approach.
- [Low-Level-Design-1/snakeandladder](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/snakeandladder/) — exact duplicate of root `snakeandladder`.
- [System-design/Snake and Ladder](<./kumaransg-LLD/Low_level_Design_Problems/System-design/Snake and Ladder/>) — Java, plain source; board/service implementation.
- [SystemDesign/SnakeAndLadder](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/SnakeAndLadder/) — Java, plain source; a separate variation whose seven files differ from `System-design/Snake and Ladder`.
- [lld/snakeandladder](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/snakeandladder/) — Java/Maven collection; entity-centric first version.
- [lld/snakeandladder_v2](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/snakeandladder_v2/) — Java/Maven collection; service-oriented second version.
- [snakeladder](./kumaransg-LLD/Low_level_Design_Problems/snakeladder/) — Java, plain source; minimal three-file version.
- [snakeladdermodified](./kumaransg-LLD/Low_level_Design_Problems/snakeladdermodified/) — Java, plain source; driver/model/service variation.
- [Problem set snakeLadder](./kumaransg-LLD/Low_level_Problem_set_2/snakeLadder/) — Java, plain source; DAO/model/service approach.
- [thegranths/SnakeAndLadder](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/SystemDesign/SnakeAndLadder/) — Java/Maven; another implementation.

## social_network

- [FacebookDesign](./kumaransg-LLD/Low_level_Design_Problems/FacebookDesign/) — database-design image only.
- [facebook](./kumaransg-LLD/Low_level_Design_Problems/facebook/) — Java/Gradle; larger Facebook service plus eight Cassandra CQL migrations.
- [FBFeed](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/FBFeed/) — empty placeholder; no solution.

## splitwise

- [LLD-Practice/Splitwise](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Splitwise/) — Java/Maven; expense and balance implementation.
- [LLD-Practice/Splitwise-Practice](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Splitwise-Practice/) — Java/Maven; separate practice implementation and requirements fixture.
- [splitwise](./kumaransg-LLD/Low_level_Design_Problems/splitwise/) — Java, plain source; expense, model, and service approach.
- [low-level-design/expense](./kumaransg-LLD/Low_level_Design_Problems/low-level-design/expense/) — Java, plain source; alternate expense-splitting implementation.
- [Problem set splitwise](./kumaransg-LLD/Low_level_Problem_set_2/splitwise/) — Java, plain source; constants, DAO, POJO, and service implementation.

## stack_overflow

- [stackoverflow](./kumaransg-LLD/Low_level_Design_Problems/stackoverflow/) — Java, plain source; questions, answers, comments, votes, users, and reputation model.
- [oops/stackoverflow](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/stackoverflow/) — Java/Maven collection; broad account, bounty, badge, moderation, search, and content model.
- [Problem set stackoverflow](./kumaransg-LLD/Low_level_Problem_set_2/stackoverflow/) — Java, plain source; constants, DAO, and model variation.

## stock_exchange

- [StockExchange](./kumaransg-LLD/StockExchange/) — Java/IntelliJ; full price-time order-matching prompt and solution.
- [StockBroking](./kumaransg-LLD/Low_level_Problem_set_2/StockBroking/) — Java, plain source; compact brokerage model variation.

## task_management

- [jira-fk](./kumaransg-LLD/Low_level_Design_Problems/jira-fk/) — Java/Maven; Jira-like stories, features, bugs, sprints, and task service.
- [trello](./kumaransg-LLD/Low_level_Problem_set_2/trello/) — Java, plain source; board/list/card-style task-management variation.

## tic_tac_toe

- [TicTacToe](./kumaransg-LLD/Low_level_Design_Problems/TicTacToe/) — Java, plain source; six-file board/game implementation.
- [System-design/TicTacToe](./kumaransg-LLD/Low_level_Design_Problems/System-design/TicTacToe/) — Java, plain source; board, logic, service, and user variation.
- [lld/tictactoe](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/tictactoe/) — Java/Maven collection; board/game/move model.
- [systemdesign/tictactoe](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/tictactoe/) — Java/Maven collection; single-file alternate implementation.
- [Problem set ticTacToe](./kumaransg-LLD/Low_level_Problem_set_2/ticTacToe/) — Java, plain source; constants, DAO, model, and service approach.
- [thegranths/TicTacToe](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/SystemDesign/TicTacToe/) — Java/Maven; fixed-board variation.
- [thegranths/TicTacToeNxN](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/SystemDesign/TicTacToeNxN/) — Java/Maven; generalized N-by-N variation.

## ticket_master

- [LLD-Practice/BookMyShow](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/BookMyShow/) — Java/Maven; booking, seat lock, payment, search, and show services.
- [bookmyshow](./kumaransg-LLD/Low_level_Design_Problems/bookmyshow/) — Java/Gradle; separate booking implementation.
- [oops/bookmyshow](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow/) — Java/Maven collection; first movie/theatre/show/seat model.
- [oops/bookmyshow2](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/oops/bookmyshow2/) — Java/Maven collection; second, broader cinema/account/catalog model.
- [movieTicketBooking](./kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking/) — Java, plain source; DAO/model implementation.
- [movieTicketBooking2](./kumaransg-LLD/Low_level_Problem_set_2/movieTicketBooking2/) — Java, plain source; controller/provider/service implementation with README.
- [System-design/BookMyShow](./kumaransg-LLD/Low_level_Design_Problems/System-design/BookMyShow/) — Java plus handwritten design image; fragment.

## vending_machine

- [System-design/VendingMachine](./kumaransg-LLD/Low_level_Design_Problems/System-design/VendingMachine/) — Java, plain source; amount, item, user, and service implementation.
- [SystemDesign/VendingMachine](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/VendingMachine/) — Java, plain source; inventory, payment, mechanical, logging, and factory services.
- [Coffee Machine question](<./kumaransg-LLD/Low_level_Design_Problems/Coffee Machine - MC Question.pdf>) — PDF prompt only; no matching source tree in this aggregate.

## voting_system

- [CountryElectionSystem](./kumaransg-LLD/Low_level_Design_Problems/CountryElectionSystem/) — Java/Gradle; election prompt PDF and implementation.

## warehouse_fulfilment_domain

- [SystemDesign/LogisticsDesign](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/LogisticsDesign/) — Java, plain source; order priority/status, items, vehicles, payment, and logistics coordination.
- [swiggyInterview1](./kumaransg-LLD/Low_level_Design_Problems/swiggyInterview1/) — Java/Maven/Spring; delivery-executive scoring and assignment, a last-mile allocation variation.

## New question candidates

- [Event_calendar_flipkart](./kumaransg-LLD/Event_calendar_flipkart/) — `event_calendar`; Java/IntelliJ plus PDF; users, teams, participants, time slots, and events.
- [designMeetingScheduler](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designMeetingScheduler/) — `event_calendar`; Java, plain source; calendars, attendees, rooms, meetings, and email.
- [systemdesign/meetingscheduler](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/systemdesign/meetingscheduler/) — `event_calendar`; Java/Maven collection; minimal alternate scheduler.
- [PropertyHunt](./kumaransg-LLD/PropertyHunt/) — `property_listing_service`; Java/IntelliJ plus complete prompt and screenshots; list, search, shortlist, unit normalization, and sale state.
- [ledger_company_navi](./kumaransg-LLD/ledger_company_navi/) — `loan_ledger_service`; Java/Maven; loans, payments, balances, and command processing.
- [leetcode-lld-flipkart-coding-blox](./kumaransg-LLD/leetcode-lld-flipkart-coding-blox/) — `online_coding_contest_platform`; Java/Maven/Spring Data JPA; users, questions, contests, scoring, leaderboard, and history.
- [Bowling-Alley-Machine-Coding-Flipkart](./kumaransg-LLD/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart/) — `bowling_alley`; Java/Maven; player, round, game, and scorekeeper variation.
- [Bowling-Alley-Machine-Coding-Flipkart-Interview](./kumaransg-LLD/Low_level_Design_Problems/Bowling-Alley-Machine-Coding-Flipkart-Interview/) — `bowling_alley`; C#/.NET Core 3.1; factory and strategy variation.
- [Problem set bowlingAlley](./kumaransg-LLD/Low_level_Problem_set_2/bowlingAlley/) — `bowling_alley`; Java, plain source; constants, DAO, and model variation.
- [Calculator](./kumaransg-LLD/Low_level_Design_Problems/Calculator/) — `calculator`; Java, plain source; command-pattern calculator.
- [Calculator-Alternative](./kumaransg-LLD/Low_level_Design_Problems/Calculator-Alternative/) — `calculator`; Java, plain source; second command/factory implementation.
- [Scheduler](./kumaransg-LLD/Low_level_Design_Problems/Scheduler/) — `job_scheduler`; Java, plain source; job/status/data-service fragment.
- [TaskSchedulerLLD](./kumaransg-LLD/Low_level_Problem_set_2/TaskSchedulerLLD/) — `job_scheduler`; Java, plain source; custom scheduler and scheduled-task model.
- [lld/jobscheduling](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/jobscheduling/) — `job_scheduler`; Java/Maven collection; task-to-machine scheduling variation.
- [HitCounter](./kumaransg-LLD/Low_level_Design_Problems/HitCounter/) — `hit_counter`; Java, plain source; counter service with Redis abstraction; fragment.
- [RuleMatcher](./kumaransg-LLD/Low_level_Design_Problems/RuleMatcher/) — `rule_engine`; Java/IntelliJ; rules, categories, containers, and logical AND matching.
- [customThreadPoolExecutor](./kumaransg-LLD/Low_level_Problem_set_2/customThreadPoolExecutor/) — `thread_pool_executor`; incomplete Java fragment with an unbounded `ArrayList` task store and reusable workers, but no task result/state handle, rejection, cancellation, or shutdown protocol; the adjacent scheduler is polling-based.
- [TextPadApplication](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/TextPadApplication/) — `text_editor`; Java/Eclipse; insert, delete, copy, paste, and print operations.
- [Text_editor_DLL](./kumaransg-LLD/Low_level_Design_Problems/Text_editor_DLL/) — `text_editor`; Java/IntelliJ; single-file doubly-linked-list variation; fragment.
- [texteditor](./kumaransg-LLD/Low_level_Design_Problems/texteditor/) — `text_editor`; Java, plain source; model/service/implementation variation.
- [Low-Level-Design-1/texteditor](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/texteditor/) — `text_editor`; exact duplicate of root `texteditor`.
- [TextPadApplication.zip](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/TextPadApplication.zip) — `text_editor`; exact archive duplicate of the extracted TextPad source; exclude.
- [designBlackJack](./kumaransg-LLD/Low_level_Design_Problems/designBlackJack/) — `blackjack`; Java, plain source; deck, card, hand, player, dealer, and game implementation.
- [Low-Level-Design-1/designBlackJack](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/designBlackJack/) — `blackjack`; exact duplicate of root `designBlackJack`.
- [deck-of-cards](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/deck-of-cards/) — `deck_of_cards`; Java, plain source; deck/card/suit/rank design with README.
- [Othello Design1.java](./kumaransg-LLD/Low_level_Design_Problems/OthelloGame/Design1.java) — `othello`; Java, single-file first design.
- [Othello Design2.java](./kumaransg-LLD/Low_level_Design_Problems/OthelloGame/Design2.java) — `othello`; Java, single-file second design; preserve alongside Design1.
- [battleship](./kumaransg-LLD/Low_level_Problem_set_2/battleship/) — `battleship`; Java, plain source; board, ship, player, and DAO implementation.
- [connect4](./kumaransg-LLD/Low_level_Problem_set_2/connect4/) — `connect_four`; Java, plain source; first board/ball implementation.
- [connect4 2](<./kumaransg-LLD/Low_level_Problem_set_2/connect4 2/>) — `connect_four`; Java, plain source; materially different second implementation.
- [connect4.zip](./kumaransg-LLD/Low_level_Problem_set_2/connect4.zip) — `connect_four`; archived third variation: 4 of its 10 Java files differ from the extracted `connect4` directory, so do not discard it as an exact duplicate.
- [SystemDesign/SnakeGame](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/SnakeGame/) — `snake_video_game`; Java, plain source; board/cell/snake/game model.
- [SystemDesign/SnakeGame2](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/SnakeGame2/) — `snake_video_game`; Java, plain source; interfaces, food service, and factory variation.
- [lld/snakegame](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/lld/snakegame/) — `snake_video_game`; Java/Maven collection; alternate board/game model.
- [smartHome](./kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome/) — `smart_home`; Java/IntelliJ; devices, commands, state, usage, DAO, and services.
- [com.flipkart.smartHome.zip](./kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome.zip) — `smart_home`; archived second variation: 3 of its 21 Java files differ from the adjacent extracted directory.
- [TrueCaller1](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/TrueCaller1/) — `caller_identification_service`; Java/Maven; accounts, contacts, addresses, tags, and social profiles; model-only fragment.
- [T9Dictionary](./kumaransg-LLD/Low_level_Design_Problems/T9Dictionary/) — `t9_dictionary`; Java plus data fixture; single-file implementation.
- [UrlShortern](./kumaransg-LLD/Low_level_Design_Problems/UrlShortern/) — `url_shortener`; Java, single-file fragment.
- [vacuumcleaner](./kumaransg-LLD/Low_level_Design_Problems/vacuumcleaner/) — `robotic_vacuum_cleaner`; Java, plain source; controller, request, template, and validator model.
- [Low-Level-Design-1/vacuumcleaner](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/vacuumcleaner/) — `robotic_vacuum_cleaner`; exact duplicate of root `vacuumcleaner`.
- [pendamictracker](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pendamictracker/) — `pandemic_tracker`; Java/Eclipse; disease, patient, regional counts, outcomes, and trend analysis.
- [pandemictracker.zip](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/pandemictracker.zip) — `pandemic_tracker`; exact archive duplicate of the misspelled extracted directory; exclude.
- [flipkart-interview-parser](./kumaransg-LLD/Low_level_Design_Problems/flipkart-interview-parser/) — `csv_object_mapper`; Java/Maven; annotations/reflection-based CSV-to-bean parser.
- [ErrorFinderApplication](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication/) — `binary_tree_validator`; Java/Eclipse with tests; detects degree, duplicate-edge, cycle, root, and parent errors.
- [ErrorFinderApplication.zip](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication.zip) — `binary_tree_validator`; exact archive duplicate of the extracted source; exclude.
- [SystemDesign/ReservationSystem](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ReservationSystem/) — `reservation_manager`; one Java file with no domain statement; incomplete fragment, not ready for a standalone question.

## Theory and note areas

- [Root README](./kumaransg-LLD/README.md) — machine-coding-round purpose, audience, interview expectations, curated prompt table, and source credits.
- [System-design/Comparable vs Comparator](<./kumaransg-LLD/Low_level_Design_Problems/System-design/Comparable vs Comparator/>) — Java examples for ordering contracts.
- [System-design/Factory Design Pattern](<./kumaransg-LLD/Low_level_Design_Problems/System-design/Factory Design Pattern/>) — Java shape-factory example.
- [System-design/Inheritance](./kumaransg-LLD/Low_level_Design_Problems/System-design/Inheritance/) — abstraction, inheritance, overloading, and overriding examples.
- [System-design/SingletonConcept](./kumaransg-LLD/Low_level_Design_Problems/System-design/SingletonConcept/) — singleton example.
- [System-design/oops1](./kumaransg-LLD/Low_level_Design_Problems/System-design/oops1/) — small OOP inheritance example.
- [LLD-Practice/DecoratorPattern](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/DecoratorPattern/) — Java/Maven decorator exercise.
- [designpatterns/decorator](./kumaransg-LLD/Low_level_Design_Problems/LowLevel-design-2/src/main/java/com/system/design/designpatterns/decorator/) — Java/Maven room-service/heater decorator example.
- [GenericProgramming](./kumaransg-LLD/Low_level_Design_Problems/GenericProgramming/) — Java/IntelliJ generics and collection examples.
- [Low-Level-Design-1/GenericProgramming](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/GenericProgramming/) — exact duplicate of root `GenericProgramming`.
- [ProducerConsumerPatternUsingBlockingQueue](./kumaransg-LLD/Low_level_Design_Problems/ProducerConsumerPatternUsingBlockingQueue/) — Java concurrency exercise.
- [SingleWriteMultipleReadLock](./kumaransg-LLD/Low_level_Design_Problems/SingleWriteMultipleReadLock/) — Java read/write-lock exercise.
- [System-design/Multithreading](./kumaransg-LLD/Low_level_Design_Problems/System-design/Multithreading/) — Java multithreading examples.
- [Leetcode/G4G](./kumaransg-LLD/Low_level_Design_Problems/Leetcode/G4G/) — mostly DSA practice, not LLD; its `SystemDesign` and custom-map sections duplicate separately indexed content.
- [thegranths/DataStructures and LeetCode](./kumaransg-LLD/Low_level_Design_Problems/thegranths/src/main/java/) — mixed DSA practice plus the separately indexed system-design variants.
- [facebook CQL migrations](./kumaransg-LLD/Low_level_Design_Problems/facebook/src/main/resources/dbmigrations/cassandra/) — eight Cassandra schema migrations useful as persistence notes.

## Exact duplicate paths

- [Leetcode/G4G/SystemDesign](./kumaransg-LLD/Low_level_Design_Problems/Leetcode/G4G/src/SystemDesign/) — exact source duplicate of [SystemDesign](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/); only `.DS_Store` files differ.
- [Leetcode/G4G/DesignDataStructures](./kumaransg-LLD/Low_level_Design_Problems/Leetcode/G4G/src/DesignDataStructures/) — exact duplicate of [DesignDataStructures](./kumaransg-LLD/Low_level_Design_Problems/DesignDataStructures/).
- [Low-Level-Design-1](./kumaransg-LLD/Low_level_Design_Problems/Low-Level-Design-1/) — contains exact copies of root `GenericProgramming`, `designBlackJack`, `designLogger`, `designchess`, `designpubsub`, `multilevelcache`, `multilevelparkinglot`, `parkinglot_services`, `snakeandladder`, `texteditor`, and `vacuumcleaner`; its `designMeetingScheduler` is unique.
- [Machine_coding_FLIPKART ZIP files](./kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/) — Error Finder, Order Booking, TextPad, Pandemic Tracker, and Driver/Rider archives repeat extracted source trees.
- [connect4.zip](./kumaransg-LLD/Low_level_Problem_set_2/connect4.zip) — near-duplicate, not exact: 4 of 10 Java sources differ from `Low_level_Problem_set_2/connect4`; preserve it as an archived variation.
- [com.flipkart.smartHome.zip](./kumaransg-LLD/Low_level_Design_Problems/smartHome/com.flipkart.smartHome.zip) — near-duplicate, not exact: 3 of 21 Java sources differ from the adjacent extracted directory; preserve it as an archived variation.

Exact duplicates should remain untouched in the reference clone but must not appear as separate canonical questions.

## Incomplete and non-solution material

- [FBFeed](./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/FBFeed/) and [Uber-Ola Design](<./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Uber-Ola Design/>) are empty placeholders.
- [ReservationSystem](./kumaransg-LLD/Low_level_Design_Problems/SystemDesign/ReservationSystem/) is a one-file fragment with no requirements.
- [Amazon](./kumaransg-LLD/Low_level_Design_Problems/Amazon/) and [FacebookDesign](./kumaransg-LLD/Low_level_Design_Problems/FacebookDesign/) are diagram-only references.
- [Parking Lot Design](<./kumaransg-LLD/Low_level_Design_Problems/LLD-Practice/Parking Lot Design/>) is requirements-only.
- [Coffee Machine question](<./kumaransg-LLD/Low_level_Design_Problems/Coffee Machine - MC Question.pdf>) is a PDF-only prompt.
- `ridesharing1`, `PlaneReservation.java`, `HitCounter`, `Text_editor_DLL`, `T9Dictionary`, `UrlShortern`, and the two single-file online-reader designs are useful fragments, not production-complete solutions.
- [low-level-design-3 README](./kumaransg-LLD/Low_level_Design_Problems/low-level-design-3/README.md) advertises a cab-booking example that is absent from that directory.

## Outbound-link-only prompts

The following names occur in the [root README's frequently asked table](./kumaransg-LLD/README.md#frequently-asked-problems), but their linked solutions are not stored in this clone:

- [2048 Game](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [AWS Lambda](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Game Engine](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Newsletter Service](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Gmail](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [WhatsApp](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Tinder](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Zoom](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Google Docs collaborative editor](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Mentorship Platform](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Cryptocurrency Exchange](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [CodePair](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Real-time Chat](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Dropbox](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Music Recognition](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [BitTorrent](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only and listed twice upstream.
- [Distributed Search](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Google Maps](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Twitter/X](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Blockchain](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Video Streaming](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only.
- [Spotify](./kumaransg-LLD/README.md#frequently-asked-problems) — outbound-link only, overlaps `music_streaming_service`, and is listed twice upstream.

## Language and build profile

| Authored/reference material | Count | Shape |
| --- | ---: | --- |
| Java source | 2,505 | Plain `javac`, Maven, Gradle, Spring/Spring Boot, and IDE-layout projects |
| C# source | 9 | Bowling Alley .NET Core 3.1 solution; two additional `.cs` files under `obj/` are generated |
| Cassandra CQL | 8 | Facebook schema migrations |
| Markdown | 53 | Prompts, requirements, build notes, and project summaries; one generated copy under `out/` excluded from this count |
| PDF | 5 | Event Calendar, Coffee Machine, Election, Ride Sharing, and Coding Blox prompts |
| PNG/JPEG/JPG/GIF | 16 | Class diagrams, handwritten notes, question images, and one decorative GIF |

There is no authored Kotlin source; the five `.kotlin_module` files are generated compiler output.

## Generated and dependency artifacts

The reference clone intentionally preserves upstream state. A curated import should exclude:

- `.git`, `.DS_Store`, `.idea`, `.vs`, `.settings`, `.gradle`, `out`, `target`, `bin`, `obj`, and compiled `classes` directories.
- `*.iml`, `.classpath`, `.project`, `*.class`, `*.kotlin_module`, `*.dll`, `*.exe`, `*.pdb`, `*.sqlite`, `*.suo`, `*.cache`, `*.bin`, `*.pb`, and `*.lst`.
- Vendored or generated `*.jar` files, including Gradle/Maven wrapper JARs and the `leetcode-lld-flipkart-coding-blox/lib` Java EE dependencies.
- Five Machine Coding ZIPs whose Java sources exactly match extracted trees. The Connect Four and Smart Home ZIPs are near-duplicates with divergent Java files and must remain indexed as alternate variations.

The pinned clone contains 4,171 non-`.git` files and occupies roughly 46 MB. A conservative audit identified 977 artifact-like files across 77 generated, IDE, cache, or output directories. Keep those facts in the provenance record; do not surface the artifacts as solution variants.
