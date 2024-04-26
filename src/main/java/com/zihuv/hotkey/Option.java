package com.zihuv.hotkey;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Option {

    private int hotKeyCnt;
    private int localCacheCnt;
    private boolean autoCache;
    private int cacheMs;
    private int minCount;
    private List<HotKey.CacheRuleConfig> whitelist = new ArrayList<>();
    private List<HotKey.CacheRuleConfig> blacklist = new ArrayList<>();
    private ILocalCache localCache;

}