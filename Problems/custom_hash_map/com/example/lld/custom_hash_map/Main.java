package com.example.lld.custom_hash_map;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        CustomHashMap<String, Integer> map = new CustomHashMap<>(2);
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 0);
        map.put("one", 11);

        System.out.println("one=" + map.get("one"));
        System.out.println("null=" + map.get(null));
        System.out.println("size=" + map.size());
    }
}
