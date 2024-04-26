package com.zihuv.hotkey;

import com.zihuv.topK.HeavyKeeper;
import com.zihuv.topK.Node;
import com.zihuv.topK.TopK;
import com.zihuv.util.Pair;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class HotKey {

    private static final String RULE_TYPE_KEY = "key";
    private static final String RULE_TYPE_PATTERN = "pattern";

    private TopK topK;
     ILocalCache localCache;
    private final Option option;
    private List<CacheRule> whitelist;
    private List<CacheRule> blacklist;

    public HotKey(Option option) {
        this.option = option;
        this.whitelist = new ArrayList<>();
        this.blacklist = new ArrayList<>();
        if (option.getHotKeyCnt() > 0) {
            int factor = (int) Math.log(option.getHotKeyCnt());
            if (factor < 1) {
                factor = 1;
            }
            this.topK = new HeavyKeeper(option.getHotKeyCnt(), 1024 * factor, 4, 0.925, option.getMinCount());
        }
        if (!option.getWhitelist().isEmpty()) {
            this.whitelist = initCacheRules(option.getWhitelist());
        }
        if (!option.getBlacklist().isEmpty()) {
            this.blacklist = initCacheRules(option.getBlacklist());
        }
        if (option.isAutoCache() || !whitelist.isEmpty()) {
            if (option.getLocalCache() != null) {
                this.localCache = option.getLocalCache();
            } else {
                this.localCache = new LocalCache(option.getLocalCacheCnt());
            }
        }
    }

    private List<CacheRule> initCacheRules(List<CacheRuleConfig> rules) {
        List<CacheRule> list = new ArrayList<>();
        for (CacheRuleConfig rule : rules) {
            int ttl = rule.getTtlMs();
            if (ttl == 0) {
                ttl = this.option.getCacheMs();
            }
            CacheRule cacheRule = new CacheRule();
            cacheRule.setTtl(ttl);

            if (Objects.equals(rule.getMode(), RULE_TYPE_KEY)) {
                cacheRule.setValue(rule.getValue());
            } else if (Objects.equals(rule.getMode(), RULE_TYPE_PATTERN)) {
                Pattern pattern = Pattern.compile(rule.getValue());
                cacheRule.setRegexp(pattern);
            }
            list.add(cacheRule);
        }
        return list;
    }

    public boolean inBlacklist(String key) {
        if (blacklist.isEmpty()) {
            return false;
        }
        for (CacheRule b : blacklist) {
            if (b.getValue().equals(key)) {
                return true;
            }
            if (b.getRegexp() != null && b.getRegexp().matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }

    public Pair<Long, Boolean> inWhitelist(String key) {
        if (whitelist.isEmpty()) {
            return new Pair<>(0L, false);
        }
        for (CacheRule b : whitelist) {
            if (b.getValue().equals(key)) {
                return new Pair<>(b.getTtl(), true);
            }
            if (b.getRegexp() != null && b.getRegexp().matcher(key).matches()) {
                return new Pair<>(b.getTtl(), true);
            }
        }
        return new Pair<>(0L, false);
    }

    public synchronized boolean addWithValue(String key, Object value, int incr) {
        boolean added = false;

        Pair<String, Boolean> result = topK.add(key, incr);
        String expelled = result.getFirst();
        added = result.getSecond();
        // 将旧节点在本地缓存剔除
        if (expelled != null && !expelled.isEmpty() && localCache != null) {
            localCache.remove(expelled);
        }
        // 判断是否要将元素添加进来
        if (option.isAutoCache() && added) {
            if (!inBlacklist(key)) {
                localCache.add(key, value, option.getCacheMs());
            }
            return added;
        }

        Pair<Long, Boolean> whitelistResult = inWhitelist(key);
        if (whitelistResult.getSecond()) {
            localCache.add(key, value, whitelistResult.getFirst());
        }
        return added;
    }

    public synchronized Pair<Object, Boolean> get(String key) {
        Object value = localCache.get(key);
        if (value != null) {
            return new Pair<>(value, true);
        }
        return new Pair<>(null, false);
    }

    public synchronized List<Node> listTopK() {
        return topK.list();
    }

    public synchronized List<Object> listCache() {
        return localCache.list();
    }

    public synchronized void delCache(String key) {
        localCache.remove(key);
    }

    public synchronized void fading() {
        topK.fading();
    }

    @Data
    static class CacheRule {
        private String value;
        private Pattern regexp;
        private long ttl;
    }

    @Data
    public static class CacheRuleConfig {
        private String mode;
        private String value;
        private int ttlMs;
    }
}