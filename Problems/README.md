# Low-Level Design Problem Bank

Every distinct problem has one canonical home. A page may contain Java/Python implementations, a design specification, or both; its README states the actual status and boundaries.

Prepare with the [60-minute Senior/Staff LLD interview playbook](../Foundations/senior_staff_lld_interview.md), then use each problem as a timed question: clarify scope, state invariants, model behavior, walk a failure path, justify SOLID/pattern choices, and finish with deterministic tests.

The eight featured pages merge every materially different cloned prompt or implementation into 47 numbered interview variations with their own expected solutions; they do not make the learner assemble an answer from a reference appendix. The clean clones and mapping documents under [References](../References/) remain the exact provenance archive, and duplicate ports/fragments are labeled instead of becoming duplicate questions.

## LLD

The requested commerce/platform set and every deduplicated source-backed addition live in the single bank below. Distinct requirement or implementation variations stay on the canonical page instead of becoming duplicate cards.

### Commerce, retail, and logistics

- [E-Commerce Platform](e_commerce/) — umbrella checkout workflows and embedded examples.
- [Online Bookstore](online_bookstore/) — catalog, patron, inventory, and order processing.
- [Product Catalog Service](product_catalog_service/) — authoritative product metadata and lifecycle.
- [Product Search System](product_search_system/) — discovery, filters, sorting, and relevance.
- [Shopping Cart with Expiration](shopping_cart_with_expiration/) — cart lifecycle and time-based expiry.
- [Order Management System](order_processing_system/) — order lifecycle and downstream coordination.
- [Inventory Reservation Service](inventory_reservation_service/) — atomic stock holds, confirmation, release, and expiry.
- [Coupon / Promotion Engine](coupon_promotion_engine/) — eligibility, stacking, discount calculation, and redemption.
- [Payment Processing Service](payment_processing_service/) — authorize, capture, void, refund, and provider callbacks.
- [Warehouse Fulfilment Domain](warehouse_fulfilment_domain/) — allocate, pick, pack, and ship.
- [Sales Analytics Dashboard](sales_analytics_dashboard/) — event aggregation and metrics.
- [Food Delivery Service](food_delivery_service/) — restaurant order and courier lifecycle.
- [Restaurant Management System](restaurant_management_system/) — reservations, tables, kitchen, and billing.
- [Property Listing / Property Hunt Service](property_listing_service/) — listings, typed measurements, search, shortlist, and sale lifecycle.
- [Package Locker Service](package_locker/) — size-aware compartment allocation, pickup codes, expiry, and release.

### Finance and trading

- [ATM System](atm_system/) — authentication, account operations, and cash dispensing.
- [Digital Wallet Service](digital_wallet_service/) — accounts, ledger entries, transfers, and funding methods.
- [Online Stock Exchange](stock_exchange/) — orders, matching, portfolios, and trade settlement.
- [Splitwise](splitwise/) — expense allocation, balances, and settlement.
- [Loan Ledger / EMI Management Service](loan_ledger_service/) — schedules, immutable ledger entries, payments, and balances.

### Booking, facilities, and education

- [Airline Management System](airline_management_system/) — flights, seats, passengers, and booking.
- [Ticket Booking / Ticket Master](ticket_master/) — movie or concert inventory, seat holds, and booking.
- [Car Rental System](car_rental_system/) — vehicles, availability, reservations, and payment.
- [Hotel Management System](hotel_management/) — rooms, stays, services, and billing.
- [Library Management System](library_management_system/) — catalog, members, loans, and fines.
- [Course Registration System](course_registration_system/) — sections, prerequisites, capacity, and waitlists.
- [Online Learning Platform](online_learning_platform/) — courses, enrollment, progress, and assessment.
- [Parking Lot](parking_lot/) — allocation, tickets, availability, and pricing.
- [Elevator System](elevator_system/) — requests, dispatch, movement, and door state.
- [Event Calendar / Meeting Scheduler](event_calendar/) — invitations, free slots, room allocation, and versioned changes.
- [Online Coding Contest Platform](online_coding_contest_platform/) — contests, submissions, judging, scoring, and leaderboards.
- [Hospital Appointment System](hospital_appointment_system/) — doctor schedules, slot holds, bookings, and cancellation.
- [Online Assessment Platform](online_assessment_platform/) — versioned tests, autosave/resume, scoring, and ranked cohorts.

### Infrastructure and reusable components

- [Cache](cache/) — bounded storage and eviction policies.
- [Multi-Level Cache](multi_level_cache/) — coordinated cache tiers and usage metrics.
- [Custom Hash Map](custom_hash_map/) — buckets, collisions, resize, and map semantics.
- [Rate Limiter](rate_limiter/) — keys, policies, decisions, and concurrent quota updates.
- [Publish-Subscribe System](pub_sub_system/) — topics, publishers, subscribers, and delivery.
- [Notification Framework](notification_framework/) — templates, preferences, channels, retries, and status.
- [Logging](logging/) — log levels, processors, appenders, and extensibility.
- [File System](file_system/) — files, directories, traversal, and operations.
- [JSON Parser](json_parser/) — tokenization, typed values, parsing, and serialization.
- [In-Memory Relational Database](in_memory_relational_database/) — schemas, rows, indexes, queries, and transactions.
- [Extensible Calculator](calculator/) — operation strategies, parsing boundaries, history, and undo decisions.
- [Job / Task Scheduler](job_scheduler/) — timed work, bounded execution, retries, and graceful shutdown.
- [Hit Counter](hit_counter/) — rolling windows, bucket boundaries, concurrent increments, and eviction.
- [Generic Rule Engine / Rule Matcher](rule_engine/) — typed facts, composable conditions, explanations, and actions.
- [Custom Thread Pool Executor](thread_pool_executor/) — bounded queues, workers, futures, rejection, and lifecycle.
- [Authentication and Authorization Service](authentication_authorization_service/) — challenges, OTP/OAuth, sessions, revocation, and policy.
- [Webhook Dispatcher](webhook_dispatcher/) — subscriptions, signed delivery, retry schedules, dedupe, and dead letters.
- [Distributed ID Generator](distributed_id_generator/) — uniqueness, ordering, clock safety, and node coordination.
- [Key-Value Store with Indexes](key_value_store/) — typed records, secondary indexes, atomic writes, and recovery.
- [Coordination Service](coordination_service/) — sessions, leases, watches, ephemeral nodes, and fencing tokens.
- [Monitoring and Alerting Platform](monitoring_alerting_platform/) — metric ingestion, rules, incidents, silences, and notification.
- [Configuration Management Service](configuration_management_service/) — versioned configuration, subscriptions, rollout, and rollback.
- [Resumable Upload Service](resumable_upload_service/) — chunk sessions, integrity, retry, completion, and cleanup.
- [T9 Dictionary](t9_dictionary/) — keypad encoding, prefix lookup, ranking, and dictionary updates.
- [URL Shortener](url_shortener/) — key generation, redirects, aliases, expiry, and analytics boundaries.
- [CSV Object Mapper](csv_object_mapper/) — schema/annotation binding, typed conversion, diagnostics, and streaming.
- [Binary Tree Validator](binary_tree_validator/) — edge validation, parent constraints, cycles, roots, and diagnostics.

### Social, collaboration, and content

- [Social Network](social_network/) — profiles, friendship, posts, feeds, messaging, and privacy.
- [Professional Network / LinkedIn](linkedin/) — career profiles, connections, jobs, and applications.
- [Stack Overflow](stack_overflow/) — questions, answers, votes, tags, and reputation.
- [Task Management](task_management/) — task ownership, status, priority, reminders, and history.
- [Music Streaming Service](music_streaming_service/) — catalog, playlists, library, and playback sessions.
- [Cricket Information System](cricinfo/) — fixtures, delivery events, live scores, and scorecards.
- [Online Auction System](online_auction_system/) — listings, bids, proxy rules, and closure.
- [Online Book Reader](online_book_reader/) — entitlements, reading sessions, progress, bookmarks, and offline merge.
- [Text Editor](text_editor/) — edit commands, ranges, clipboard, history, undo, and redo.
- [Survey and Form Builder](survey_form_builder/) — versioned forms, branching, responses, validation, and publication.
- [Chat and Messaging Service](chat_messaging_service/) — conversations, delivery, ordering, receipts, and reconnect.
- [Document Collaboration Platform](document_collaboration_platform/) — access control, operations, revisions, presence, and convergence.
- [Caller Identification Service](caller_identification_service/) — contacts, normalized numbers, tags, privacy, and spam reputation.

### Mobility, games, machines, and civic systems

- [Cab Booking / Ride Sharing](cab_booking_service/) — riders, drivers, matching, trips, and fares.
- [Chess](chess_game/) — legal moves, king safety, history, and game outcomes.
- [Snake and Ladder](snake_ladder/) — board transitions, turn rotation, and game sessions.
- [Tic-Tac-Toe](tic_tac_toe/) — board, turns, legal moves, winner, and draw.
- [Vending Machine](vending_machine/) — selection, inventory, payment, dispensing, and configurable coffee recipes.
- [Traffic Signal Control](traffic_signal/) — signal cycles, intersections, and emergency priority.
- [Voting System](voting_system/) — eligibility, secret ballots, duplicate prevention, and tallying.
- [Bowling Alley](bowling_alley/) — frames, legal rolls, strike/spare bonuses, lanes, and scorecards.
- [Blackjack](blackjack/) — deck/shoe, hands, dealer policy, wagers, outcomes, and settlement.
- [Deck of Cards](deck_of_cards/) — card identity, deck composition, shuffle, deal, and reusable game policies.
- [Othello / Reversi](othello/) — legal directional captures, turns, pass rules, scoring, and two model variations.
- [Battleship](battleship/) — fleet placement, hidden boards, attacks, hit state, and winner detection.
- [Connect Four](connect_four/) — column gravity, turns, win detection, draw state, and alternate boards.
- [Snake Video Game](snake_video_game/) — movement ticks, growth, collisions, food policy, and game loop.
- [Smart Home](smart_home/) — devices, commands, schedules, state, usage, and automation rules.
- [Jukebox](jukebox/) — credits, selection queue, playback state, and device adapters.
- [Maps Navigation Service](maps_navigation_service/) — route alternatives, travel modes, guidance, and rerouting alerts.
- [Robotic Vacuum Cleaner](robotic_vacuum_cleaner/) — map/grid state, movement policy, cleaning commands, and recovery.
- [Pandemic Tracker](pandemic_tracker/) — disease cases, regional aggregates, outcomes, and trend reporting.

## Canonical aliases

These names intentionally resolve to an existing problem instead of creating duplicates:

| Alternate name | Canonical page |
| --- | --- |
| Coffee Vending Machine | [Vending Machine](vending_machine/) |
| Concert Ticket Booking / Movie Ticket Booking | [Ticket Booking](ticket_master/) |
| Ride-Sharing Service | [Cab Booking](cab_booking_service/) |
| Online Shopping Service | [E-Commerce](e_commerce/) |
| Online Stock Brokerage | [Online Stock Exchange](stock_exchange/) |
| LRU Cache | [Cache](cache/) |
| File and Directory System | [File System](file_system/) |
| Social Networking Service | [Social Network](social_network/) |
| Meeting Scheduler / Event Calendar | [Event Calendar](event_calendar/) |
| Property Hunt | [Property Listing Service](property_listing_service/) |
| Rule Matcher / Business Rules Engine | [Rule Engine](rule_engine/) |
| Custom Executor / Thread Pool | [Thread Pool Executor](thread_pool_executor/) |
| Truecaller / Caller ID Directory | [Caller Identification Service](caller_identification_service/) |
| TextPad | [Text Editor](text_editor/) |
| Connect4 | [Connect Four](connect_four/) |
| UUID / Unique ID Service | [Distributed ID Generator](distributed_id_generator/) |
| Online Exam / Quiz Platform | [Online Assessment Platform](online_assessment_platform/) |
| Google Docs / Collaborative UML Editor | [Document Collaboration Platform](document_collaboration_platform/) |

## Expected shape of a solution

A complete submission should identify scope, functional requirements, core entities and value objects, state transitions, invariants, APIs, concurrency boundaries, failure handling, design choices, demo scenarios, and extension points. If code is added, include deterministic tests or a runnable demo and document the exact compile/run command.

The [interview notes](interview/) provide an additional design walkthrough, and the root [Foundations](../Foundations/) and [UML](../UML/) sections cover prerequisite techniques.
