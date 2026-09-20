# T9 Dictionary

## Prompt and scope

Design a predictive T9 dictionary that loads alphabetic words, maps each letter to keypad digits `2` through `9`, and returns word suggestions for a digit sequence. The source implements a digit-keyed ternary trie and returns the first word stored at a matching terminal.

Sentence composition, autocorrection, personalization, and multilingual input are outside the first iteration.

## Core model

- `Trie`: singleton dictionary, letter-to-digit mapping, insertion, and lookup.
- `Node`: one keypad digit, left/middle/right links, and terminal words.
- Dictionary loader: reads tokens from a fixture and inserts alphabetic words.
- Suggestion policy: chooses and orders words sharing the same digit sequence.

## Invariants

- Accepted words are non-empty alphabetic strings normalized to one case.
- Query patterns contain only digits `2` through `9`; every traversed node exists before it is dereferenced.
- A terminal may hold several words because T9 mappings collide.
- Suggestions are unique and deterministically ranked; a limit does not change ordering.
- Dictionary publication is atomic: readers see either the previous complete index or the next one.

## API

The source exposes `Trie.getInstance()`, `add(String)`, and `getWord(String)`. A fuller boundary is:

```text
load(Reader words) -> DictionaryVersion
add(word)
remove(word)
suggest(digits, limit) -> List<String>
contains(word) -> boolean
```

Return an empty list or typed validation error instead of relying on nulls or terminal-list indexing.

## Main flow

1. Normalize a word and translate each letter to its keypad digit.
2. Insert digits into the ternary trie: lower digit goes left, equal advances middle, higher goes right.
3. Store the normalized word at the terminal node.
4. Traverse the same decisions for a query and rank the terminal candidates.

For `L` characters, lookup is `O(L)` on the middle path plus ternary sibling comparisons; ranking adds work proportional to returned candidates.

## Concurrency and failure handling

- Reject `0`, `1`, punctuation, unknown words, empty dictionaries, unreadable input, and unsupported encodings explicitly.
- The source singleton is lazily initialized without a `volatile` instance and its mutable trie is not safe for concurrent reads and writes.
- Prefer an immutable published trie or protect updates with a read/write strategy. Keep loading off the query path.
- Bound candidate lists and define behavior for duplicate insertions and hot-prefix abuse.

## Design solution

Use a trie keyed directly by digits when T9 lookup dominates; retain terminal candidate metadata such as frequency and recency. Separate dictionary storage from ranking so frequency, locale, or user history can change without changing traversal. A standard digit trie is simpler than a ternary tree for the fixed eight-key alphabet, while the source's ternary representation reduces sparse child arrays.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| Single-file partial implementation | [`References/kumaransg-LLD/Low_level_Design_Problems/T9Dictionary/T9Dictionary.java`](../../References/kumaransg-LLD/Low_level_Design_Problems/T9Dictionary/T9Dictionary.java) | [file at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/T9Dictionary/T9Dictionary.java) | Ternary digit trie, loader, memory timing, and first-match lookup. |
| Data fixture | [`References/kumaransg-LLD/Low_level_Design_Problems/T9Dictionary/data.txt`](../../References/kumaransg-LLD/Low_level_Design_Problems/T9Dictionary/data.txt) | [file at `1698cc6`](https://github.com/kumaransg/LLD/blob/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/T9Dictionary/data.txt) | One sentence used as dictionary input; not a second approach. |

## Follow-ups

- Rank collisions by language frequency and per-user recency.
- Return prefix completions before all digits are entered.
- Support Unicode alphabets and configurable keypad layouts.
- Persist compact tries, hot-swap dictionary versions, and benchmark memory.

## Implementation status

**Compilable but non-portable reference; no code copied here.** The source compiles with a deprecation warning, but its main fails because it opens a hard-coded path from the original author's machine instead of the adjacent fixture. Unknown patterns can also dereference missing nodes, lookup returns only the first collision, and there are no tests.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
