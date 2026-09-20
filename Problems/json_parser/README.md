# JSON Parser

## Problem

Design a dependency-free JSON module that converts text into a typed value tree and serializes that tree back to valid JSON.

## Required behavior

- Tokenize objects, arrays, strings, numbers, booleans, and `null` while tracking source position.
- Decode escapes, including Unicode escapes and surrogate pairs.
- Parse nested values with useful errors for unexpected tokens, invalid numbers, bad escapes, and premature end of input.
- Represent results with a closed value hierarchy such as `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBoolean`, and `JsonNull`.
- Serialize compact or pretty-printed output with correct escaping.
- Offer strict duplicate-key and numeric-range policies rather than silently choosing behavior.

## Design focus

Separate character input, tokenization, recursive-descent parsing, the value model, and writing. The tokenizer owns lexical correctness; the parser owns grammar and nesting. Avoid using a map of `Object`, which loses the closed JSON type system.

## Invariants and edge cases

- After parsing one root value, only whitespace may remain.
- Nesting depth and input size must be bounded for untrusted input.
- Numbers follow JSON grammar: no leading plus sign, illegal leading zero, `NaN`, or infinity.
- Preserve object insertion order if stable serialization is promised.

## Design exercise

Demonstrate every value type, nesting, escapes, round-trip serialization, duplicate keys, malformed input with line/column errors, depth limits, and streaming from a `Reader`.

## Extensions

Streaming events, JSON Pointer, schema validation, mapping records through adapters, and incremental parsing.
