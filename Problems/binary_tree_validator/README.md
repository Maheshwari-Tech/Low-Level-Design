# Binary Tree Validator

## Prompt and scope

Given directed tuples of the form `(parent, child)`, validate whether they can form one binary tree and report structural errors. The source names five checks: more than two children, duplicate tuples, cycle, multiple roots, and multiple parents.

Tree balancing, key ordering, traversal, persistence, and editing are outside this validator.

## Core model

- `Pair`: directed parent-child edge.
- Edge set and adjacency structure: source uses a fixed `256 x 256` matrix.
- Indegree/outdegree calculators: derive parent and child-count constraints.
- Cycle detector: depth-first traversal with recursion-stack state.
- `ErrorDetector`: accumulates error labels `E1` through `E5`.
- `ValidationResult`: canonical typed outcome containing root, ordered errors, and optional normalized tree.

## Invariants

- Every edge is well formed and unique; node identifiers belong to the accepted alphabet.
- Each node has at most one parent and each parent has at most two distinct children.
- A non-empty valid graph has exactly one root, is acyclic, and every observed node is reachable from that root.
- A single-node input policy is explicit even when no edge mentions the node.
- Error ordering or precedence is deterministic and documented; the source accumulates multiple labels.

## API

```text
validate(Collection<Edge> edges) -> ValidationResult

ValidationResult:
  isValid()
  root()
  errors()   // DUPLICATE_EDGE, TOO_MANY_CHILDREN, CYCLE,
             // MULTIPLE_ROOTS, MULTIPLE_PARENTS, DISCONNECTED, MALFORMED_INPUT
```

Parsing text and validating the graph should be separate functions.

## Main flow

1. Parse and validate tuple syntax; insert each edge into a set to detect duplicates.
2. Build adjacency plus indegree/outdegree maps for every observed node.
3. Record degree violations and identify zero-indegree root candidates.
4. Run three-color DFS from every component to detect cycles.
5. If exactly one root exists, traverse from it and verify all nodes are reachable.
6. Return errors in the chosen precedence or sorted order without printing inside the domain service.

Time and space are `O(V + E)` with adjacency lists and maps.

## Concurrency and failure handling

- A validator invocation should use local immutable input and local graph state, making independent calls naturally thread-safe.
- Reject malformed tuples, unsupported labels, self-loops, oversized input, duplicate edges, and integer/index overflow before traversal.
- Use iterative DFS or enforce depth limits for adversarial graphs.
- Never return a partial tree as valid when a detector fails; parsing and algorithm errors are typed separately.

## Design solution

Represent edges as value objects, store adjacency in `Map<Node, Set<Node>>`, and calculate degrees during one build pass. Compose independent rules behind a validator pipeline, but share the normalized graph so rules do not rebuild it. A three-color DFS (`WHITE`, `GRAY`, `BLACK`) correctly distinguishes a back edge from an already completed node. Reachability closes the gap left by checking only root count.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Partial extracted implementation | [`References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication`](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication) | Interactive parser, degree checks, DFS attempt, and compiled output. |
| **Exact duplicate archive** | [`References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication.zip`](../../References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication.zip) | [ZIP at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/ErrorFinderApplication.zip) | Archive copy of the extracted source; not a second solution. |

## Follow-ups

- Support arbitrary string node IDs and deterministic child ordering.
- Return a normalized immutable tree after successful validation.
- Compare fail-fast error precedence with complete error accumulation.
- Add property-based tests over random graphs and deep non-recursive traversal.

## Implementation status

**Compilable but unreliable reference; no code copied here.** The interactive source compiles, but the cycle DFS indexes adjacency incorrectly and its recursion-stack membership check compares array values to node indices. Root detection misses some cases, input assumes fixed character positions, scanners are repeatedly opened, and every file under `test/` is commented out rather than executable.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
