# Custom HashMap

## Scope

This educational implementation shows the core mechanics behind a hash table: bucket indexing, separate chaining for collisions, key replacement, load-factor tracking, and resize-time rehashing.

## Design

- `CustomHashMap<K, V>` exposes `put`, `get`, and `size`.
- An array stores linked `Entry` chains, giving average-case O(1) lookup and update.
- Capacity doubles after the load factor exceeds 0.75.
- Hash spreading plus `Math.floorMod` handles negative hash codes; a null key is stored in bucket zero.

The implementation is intentionally smaller than `java.util.HashMap`: it does not provide iterators, views, removal, ordering, or concurrent access.

## Run

```bash
javac Problems/custom_hash_map/com/example/lld/custom_hash_map/*.java
java -cp Problems/custom_hash_map com.example.lld.custom_hash_map.Main
```

Implementation: [`CustomHashMap.java`](com/example/lld/custom_hash_map/CustomHashMap.java)
