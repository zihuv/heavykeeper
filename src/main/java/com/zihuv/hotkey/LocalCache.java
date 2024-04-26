package com.zihuv.hotkey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
public class LocalCache implements ILocalCache {

    private final LRUCache<String, Item> cache;
    private final Long startTime;

    public LocalCache(int cap) {
        cache = new LRUCache<>(cap);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void add(String key, Object value, long ttl) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = ttl + (currentTime - startTime);
        Item item = new Item(expirationTime, value);
        cache.put(key, item);
    }

    @Override
    public Object get(String key) {
        Item item = cache.get(key);
        if (item != null) {
            long currentTime = System.currentTimeMillis();
            if (item.getTtl() > (currentTime - startTime)) {
                return item.getValue();
            }

            cache.remove(key);
        }
        return null;
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public List<Object> list() {
        // 仅用作测试，不推荐正式环境使用
        Set<Map.Entry<String, Item>> entries = cache.entrySet();
        return new ArrayList<>(entries);
    }

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        public final int cap;

        public LRUCache(int cap) {
            super(cap, 0.75f, true);
            this.cap = cap;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > cap;
        }

    }
//    @Data
//    static class LRUCache<K, V> {
//
//        private final Map<K, V> cacheMap;
//
//        public LRUCache(final int cacheSize) {
//            this.cacheMap = Collections.synchronizedMap(new LinkedHashMap<>(cacheSize, 0.75f, true) {
//                @Override
//                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
//                    return size() > cacheSize;
//                }
//            });
//        }
//
//        public void put(K key, V value) {
//            cacheMap.put(key, value);
//        }
//
//        public V get(K key) {
//            return cacheMap.get(key);
//        }
//
//        public void remove(K key) {
//            cacheMap.remove(key);
//        }
//
//    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Item {
        private Long ttl;
        private Object value;
    }
}