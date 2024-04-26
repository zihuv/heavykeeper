package com.zihuv.hotkey;

import java.util.List;

public interface ILocalCache {

    void add(String key, Object value, long ttl);

    Object get(String key);

    void remove(String key);

    List<Object> list();
}