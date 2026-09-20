# Low Level Design

A Java-first collection of object-oriented design notes, patterns, concurrency exercises, and interview problems. The repository favors explicit invariants, small interfaces, valid state transitions, and runnable examples where an implementation is present.

The companion [LLD Atlas learning platform](website/) connects foundations, SOLID, all 23 GoF patterns, concurrency, UML, interview questions, runnable examples, and a searchable exact-revision source library.

## Start here

1. [Senior/Staff interview playbook](Foundations/senior_staff_lld_interview.md) — a complete 60-minute answer method, SOLID/pattern reasoning, follow-ups, and scoring.
   - [Amazon/Coupang strong-hire guide](Foundations/amazon_coupang_lld_strong_hire.md) — company signals, seven-problem map, concurrency/retry talk tracks, and mock order.
2. [Design principles](Principles/) — SOLID and supporting principles.
3. [Design patterns](Patterns/) — the complete GoF catalog with local examples.
4. [UML](UML/) — class, use-case, sequence, activity, and state-machine diagrams.
5. [Concurrency](Concurrency/) — Java memory, coordination, failure modes, patterns, and exercises.
   - [Runnable Java interview patterns](Concurrency/interview_patterns/) — non-blocking retry, bounded producer-consumer dispatch, async Observer, and backpressure.
6. [LLD problem bank](Problems/) — 92 canonical questions with complete explanations and source where implemented.
7. [Further resources](RESOURCES.md) — courses, books, and external collections from the reference catalog.
8. [Attributed example archive](Examples/awesome-low-level-design/) — the imported multi-language upstream repository, diagrams, license, and provenance.
9. [Exact local reference clones](References/) — three unmodified Git repositories, their verified revisions/license boundaries, and an exhaustive solution-variation index.
10. [Python Amazon/Coupang pack](Python/amazon_coupang_lld/) — runnable Python 3.9+ versions of the seven featured problems using `asyncio`, locks, queues, protocols, and dataclasses.

## LLD

Eight complete 60-minute Senior/Staff interview packs, each with a candidate-facing question, expected answer, SOLID/pattern reasoning, failure and concurrency follow-ups, scoring guidance, reference variations, and runnable Java 17 code:

- [Order Management System](Problems/order_processing_system/)
- [Inventory Reservation Service](Problems/inventory_reservation_service/)
- [Coupon / Promotion Engine](Problems/coupon_promotion_engine/)
- [Notification Framework](Problems/notification_framework/)
- [Payment Processing Service](Problems/payment_processing_service/)
- [Warehouse Fulfilment Domain](Problems/warehouse_fulfilment_domain/)
- [Rate Limiter](Problems/rate_limiter/)
- [Product Catalog Service](Problems/product_catalog_service/)

The [complete catalog](Problems/README.md) contains the maintained, deduplicated interview set. Exhaustive prompt/variation coverage—including fragments and outbound-link-only topics that are not promoted to standalone solutions—lives under [References](References/) and in the website Source Library.

## Repository structure

```text
Low level Design/
├── Foundations/    # OOP, relationships, clean code, testing
├── Principles/     # SOLID and complementary principles
├── Patterns/       # Creational, structural, behavioral
├── UML/            # Diagram selection and conventions
├── Concurrency/    # Concepts, Java examples, question bank
├── Problems/       # Interview prompts and Java implementations
├── Examples/       # Licensed, attributed upstream snapshots
├── References/     # Exact nested Git clones and provenance indexes
└── website/        # Searchable LLD Atlas and code explorer
```

Each area owns its explanation. Root navigation does not repeat the detailed theory in category pages, and aliases such as “ride sharing” and “cab booking” resolve to one canonical problem.

## How to approach a problem

1. Clarify actors, scope, and out-of-scope behavior.
2. List use cases, invariants, and failure cases before classes.
3. Model entities, value objects, state transitions, policies, and external ports.
4. Walk a success path and a compensating/failure path.
5. Address concurrency only around shared invariants.
6. Add tests for boundaries, invalid transitions, retries, and competing commands.

Implementations were collected from several self-contained projects and do not yet share one universal build command. Follow the README inside a problem and treat pages marked as design exercises as specifications rather than completed code. The eight featured commerce and platform problems above each include a strict Java 17 compile command and assertion-backed runnable demo.

## Coverage

[COVERAGE.md](COVERAGE.md) records how earlier local material maps to canonical pages. The exact clones of [`ashishps1/awesome-low-level-design`](https://github.com/ashishps1/awesome-low-level-design), [`prasadgujar/low-level-design-primer`](https://github.com/prasadgujar/low-level-design-primer), and [`kumaransg/LLD`](https://github.com/kumaransg/LLD) live under [References](References/) with pinned commits and verified clean working trees. [VARIATION_INDEX.md](References/VARIATION_INDEX.md) preserves distinct implementations while labeling exact duplicates, fragments, and outbound-link-only prompts; [PRIMER_QUESTION_INDEX.md](References/PRIMER_QUESTION_INDEX.md) accounts for every primer prompt and every linked solution/video variation.

The website indexes every tracked file in all three clones plus the maintained learning content in this workspace. Generated, IDE, and binary artifacts remain discoverable for completeness but are hidden by default. License boundaries are explicit: `awesome-low-level-design` is GPL-3.0; the primer and aggregate Kumar repository declare no repository-wide license at their pinned revisions.
