package code;

import code.policies.LRUBasedEvictionPolicy;
import code.storage.HashMapBasedStorage;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        Cache<Integer, Integer> cache = new CacheImpl<>(new HashMapBasedStorage<>(5),
                new LRUBasedEvictionPolicy<>());

    }
}