# Source Coverage

This file records the union of the local sibling repository and the external reference catalog. It also makes alias decisions visible so a future update adds only genuinely new material.

## Local sibling: `../LLD`

| Source area | Canonical target |
| --- | --- |
| Root clean-code and OOP curriculum | [Foundations](Foundations/) |
| SOLID and complementary principles | [Principles](Principles/) |
| Creational, structural, and behavioral patterns | [Patterns](Patterns/) |
| UML and top-down design workflow | [UML](UML/) |
| Multithreading curriculum and examples | [Concurrency](Concurrency/) |
| Java problem implementations and READMEs | [Problems](Problems/) |

All substantive source files are represented in the canonical areas. IDE settings, `.DS_Store`, compiled classes, Maven `target` output, and module metadata are generated or machine-specific artifacts and are deliberately excluded.

Source README-only questions map as follows:

| Source question | Canonical target |
| --- | --- |
| Food delivery like Zomato | [Food Delivery Service](Problems/food_delivery_service/) |
| Chess | [Chess](Problems/chess_game/) |
| JSON parser | [JSON Parser](Problems/json_parser/) |
| In-memory MySQL | [In-Memory Relational Database](Problems/in_memory_relational_database/) |
| Stock trading | [Online Stock Exchange](Problems/stock_exchange/) |
| E-commerce like Amazon | [E-Commerce](Problems/e_commerce/) |
| Social media like Facebook | [Social Network](Problems/social_network/) |
| Extensible caching library | [Cache](Problems/cache/) and [Multi-Level Cache](Problems/multi_level_cache/) |

## External catalog

Baseline: [`ashishps1/awesome-low-level-design`](https://github.com/ashishps1/awesome-low-level-design), snapshot at commit `fc26e4033cad6d24f32caa8521044febbf065beb`. Its [GPL-3.0 license](https://github.com/ashishps1/awesome-low-level-design/blob/main/LICENSE) applies to its code and diagrams.

The catalog is merged in two deliberate layers:

1. Original Java-oriented notes and one canonical page per distinct problem live in the main learning path.
2. The complete educational source/diagram snapshot lives under [Examples/awesome-low-level-design](Examples/awesome-low-level-design/), with the upstream README, exact GPL-3.0 license, revision, and provenance notice preserved.

This keeps alternate implementations available without duplicating problem navigation or obscuring authorship. Generated build output, dependency caches, repository metadata, binaries, and logs are excluded from the snapshot.

## Exact reference clones and full source index

All three requested upstream repositories also exist as clean, full nested Git clones under [References](References/). Their exact branches, commits, remotes, file counts, and license boundaries are verified in [References/README.md](References/README.md):

| Repository | Pinned commit | Tracked files | Canonical use |
| --- | --- | ---: | --- |
| `ashishps1/awesome-low-level-design` | `fc26e4033cad6d24f32caa8521044febbf065beb` | 3,343 | Multi-language solutions, OOP, patterns, diagrams |
| `prasadgujar/low-level-design-primer` | `49fe9f2fc2fcd409e25b20e0bed8c37337d64ebd` | 7 | Prompt variations, external solutions, resources |
| `kumaransg/LLD` | `1698cc6f993a5014d4370b5e0db9f64d322e2400` | 4,171 | Machine-coding variants, notes, diagrams, project sources |

The [Kumar variation index](References/VARIATION_INDEX.md) maps distinct implementations to one canonical question and labels exact copies, archives, fragments, generated artifacts, and outbound-link-only prompts. The [primer coverage index](References/PRIMER_QUESTION_INDEX.md) reconciles all 140 prompts plus all 28 solution topics, 40 solution links, and 24 video links. The website Source Library indexes every tracked file in the three clones and every maintained learning file in this workspace. It lazily previews notes/code and links clone content to the exact pinned GitHub revision; generated or binary artifacts remain indexed but hidden by default.

The primer and the aggregate Kumar repository declare no repository-wide license at these commits. Their clones remain unmodified reference material; workspace-owned pages summarize and link rather than silently republishing those sources as canonical code.

### Concepts

| External area | Canonical target |
| --- | --- |
| Classes, objects, enums, interfaces, encapsulation, abstraction, inheritance, polymorphism | [OOP](Foundations/object_oriented_programming.md) |
| Association, aggregation, composition, dependency | [Class relationships](Foundations/class_relationships.md) |
| DRY, YAGNI, KISS, SOLID | [Principles](Principles/) |
| Creational, structural, behavioral patterns | [Patterns](Patterns/) |
| Class, use-case, sequence, activity, state-machine diagrams | [UML](UML/) |
| Concurrency concepts, primitives, hazards, and patterns | [Concurrency](Concurrency/) |
| Nine concurrency exercises | [Concurrency question bank](Concurrency/questions/) |
| Courses, books, newsletter, and additional links | [Further resources](RESOURCES.md) |

### Interview problems

| External problem | Canonical target |
| --- | --- |
| Airline Management System | [Airline Management](Problems/airline_management_system/) |
| ATM | [ATM](Problems/atm_system/) |
| Car Rental System | [Car Rental](Problems/car_rental_system/) |
| Chess Game | [Chess](Problems/chess_game/) |
| Coffee Vending Machine | [Vending Machine](Problems/vending_machine/) |
| Concert Ticket Booking System | [Ticket Booking](Problems/ticket_master/) |
| Course Registration System | [Course Registration](Problems/course_registration_system/) |
| CricInfo | [Cricket Information](Problems/cricinfo/) |
| Digital Wallet Service | [Digital Wallet](Problems/digital_wallet_service/) |
| Elevator System | [Elevator](Problems/elevator_system/) |
| Food Delivery Service | [Food Delivery](Problems/food_delivery_service/) |
| Hotel Management System | [Hotel Management](Problems/hotel_management/) |
| Library Management System | [Library Management](Problems/library_management_system/) |
| LinkedIn | [Professional Network](Problems/linkedin/) |
| Logging Framework | [Logging](Problems/logging/) |
| LRU Cache | [Cache](Problems/cache/) |
| Movie Ticket Booking System | [Ticket Booking](Problems/ticket_master/) |
| Music Streaming Service | [Music Streaming](Problems/music_streaming_service/) |
| Online Auction System | [Online Auction](Problems/online_auction_system/) |
| Online Shopping Service | [E-Commerce](Problems/e_commerce/) |
| Online Stock Brokerage System | [Online Stock Exchange](Problems/stock_exchange/) |
| Parking Lot | [Parking Lot](Problems/parking_lot/) |
| Publish-Subscribe System | [Publish-Subscribe](Problems/pub_sub_system/) |
| Restaurant Management System | [Restaurant Management](Problems/restaurant_management_system/) |
| Ride-Sharing Service | [Cab Booking](Problems/cab_booking_service/) |
| Snake and Ladder | [Snake and Ladder](Problems/snake_ladder/) |
| Social Networking Service | [Social Network](Problems/social_network/) |
| Splitwise | [Splitwise](Problems/splitwise/) |
| Stack Overflow | [Stack Overflow](Problems/stack_overflow/) |
| Task Management System | [Task Management](Problems/task_management/) |
| Tic-Tac-Toe | [Tic-Tac-Toe](Problems/tic_tac_toe/) |
| Traffic Signal Control | [Traffic Signal](Problems/traffic_signal/) |
| Vending Machine | [Vending Machine](Problems/vending_machine/) |

The external class-diagram/solution tree adds three topics beyond its problem-markdown list: [File System](Problems/file_system/), [Online Learning Platform](Problems/online_learning_platform/), and [Voting System](Problems/voting_system/).

### Imported upstream implementations

The local archive contains the upstream [multi-language OOP examples](Examples/awesome-low-level-design/oop/), [multi-language pattern examples](Examples/awesome-low-level-design/design-patterns/), and [class-diagram images](Examples/awesome-low-level-design/class-diagrams/). The GitHub project remains the authoritative upstream source.

Its imported problem implementations are available in [C++](Examples/awesome-low-level-design/solutions/cpp/), [C#](Examples/awesome-low-level-design/solutions/csharp/), [Go](Examples/awesome-low-level-design/solutions/golang/), [Java](Examples/awesome-low-level-design/solutions/java/), [Python](Examples/awesome-low-level-design/solutions/python/), and [TypeScript](Examples/awesome-low-level-design/solutions/typescript/). Local Java implementations remain the canonical reference where they add distinct design value; upstream copies stay in the attributed namespace.
