package com.example.lld.custom_hash_map;

import java.util.Objects;

/**
 * A small educational hash map that uses separate chaining and grows when its
 * load factor exceeds 0.75. It intentionally exposes only the operations used
 * by the original example: put, get, and size.
 */
public final class CustomHashMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;

    private Entry<K, V>[] table;
    private int size;

    public CustomHashMap() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public CustomHashMap(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        table = (Entry<K, V>[]) new Entry<?, ?>[initialCapacity];
    }

    public void put(K key, V value) {
        int index = indexFor(key, table.length);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }

        table[index] = new Entry<>(key, value, table[index]);
        size++;

        if (size > table.length * LOAD_FACTOR) {
            resize();
        }
    }

    public V get(K key) {
        Entry<K, V> current = table[indexFor(key, table.length)];
        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] oldTable = table;
        table = (Entry<K, V>[]) new Entry<?, ?>[oldTable.length * 2];

        for (Entry<K, V> head : oldTable) {
            Entry<K, V> current = head;
            while (current != null) {
                Entry<K, V> next = current.next;
                int newIndex = indexFor(current.key, table.length);
                current.next = table[newIndex];
                table[newIndex] = current;
                current = next;
            }
        }
    }

    private int indexFor(K key, int capacity) {
        int hash = key == null ? 0 : key.hashCode();
        hash ^= hash >>> 16;
        return Math.floorMod(hash, capacity);
    }

    private static final class Entry<K, V> {
        private final K key;
        private V value;
        private Entry<K, V> next;

        private Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
}
