# Generic Rule Engine / Rule Matcher

## Interview Prompt

Design a small rules engine that evaluates typed facts against composable conditions, explains why a rule matched, and executes deterministic actions without hard-coded domain branches.

## Scope and Requirements

1. Define atomic comparisons over named, typed facts.
2. Compose conditions using `ALL`, `ANY`, and `NOT`.
3. Assign rule priority, effective interval, and enabled/version state.
4. Evaluate a fact set deterministically and return match explanations.
5. Support first-match, all-match, and conflict-resolution policies.
6. Keep side-effecting actions separate from pure evaluation.

## Core Model

| Type | Responsibility |
| --- | --- |
| `FactKey<T>` / `Facts` | Typed evaluation input |
| `Condition` | Pure predicate returning an explained result |
| `CompositeCondition` | AND/OR/NOT tree |
| `Rule` | Identity, version, priority, condition, and action references |
| `RuleSet` | Versioned ordered rules and conflict policy |
| `EvaluationResult` | Matched rules plus an explanation tree |
| `ActionPort` | Executes chosen effects after evaluation commits |

## Invariants

- Evaluation is pure for the same rule-set version, facts, and clock.
- Rules use declared fact types; missing facts follow an explicit policy.
- Ordering is stable: priority, rule ID, then version.
- One evaluation ID executes a selected action at most once.
- Cycles are impossible because a condition graph is built as a tree/DAG and validated.

## API Sketch

```java
RuleSetVersion publish(DraftRuleSet draft, long expectedVersion);
EvaluationResult evaluate(RuleSetId id, Facts facts, EvaluationMode mode);
ActionReceipt execute(EvaluationResult result, String idempotencyKey);
Explanation explain(EvaluationId evaluationId);
```

## Design Solution

Model atomic and composite conditions with Specification/Composite. Validate and compile a draft rule tree when publishing so runtime evaluation is allocation-light and cannot fail on bad structure. Treat actions as names/commands returned by evaluation; an orchestrator executes them through ports after persisting the chosen result and idempotency key. Version rule sets so past decisions remain reproducible.

## Source-Backed Variation

- `References/kumaransg-LLD/Low_level_Design_Problems/RuleMatcher/` — Java/IntelliJ rules, categories, containers, and logical-AND matching. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/RuleMatcher)

The [Coupon / Promotion Engine](../coupon_promotion_engine/README.md) is a domain-specific use of these ideas; this page keeps the reusable evaluation framework separate.

## Follow-Ups

Temporal/aggregate facts, decision tables, expression parsing, rule rollout, caching, audit retention, and safe user-authored scripts.

## Implementation Status

The page provides the canonical design; the actual upstream matcher variation is preserved in the local clone.
