package com.zihuv.hotkey;

import org.apache.commons.math3.distribution.ZipfDistribution;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class HotKeyTest {

    @Test
    public void test01() throws InterruptedException {
        benchmarkHotkey(true, 0.1, new ArrayList<>());
    }

    public void benchmarkHotkey(boolean autoCache, double writePercent, List<HotKey.CacheRuleConfig> whitelist) throws InterruptedException {
        Option option = new Option();
        option.setHotKeyCnt(100);
        option.setLocalCacheCnt(100);
        option.setAutoCache(autoCache);
        option.setCacheMs(100);
        option.setWhitelist(whitelist);

        HotKey h = new HotKey(option);

        ZipfDistribution zipf = new ZipfDistribution(1000, 2.0);
        // 重置计时器
        long startTime = System.nanoTime();

        // 并行执行测试
        int numThreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                Random random = ThreadLocalRandom.current();
                while (System.nanoTime() - startTime < TimeUnit.SECONDS.toNanos(1)) {
                    long key = zipf.sample();
                    if (random.nextDouble() < writePercent) {
                        h.addWithValue(String.valueOf(key), String.valueOf(key), 1);
                    } else {
                        h.get(String.valueOf(key));
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有线程结束
        for (Thread thread : threads) {
            thread.join();
        }
        // 打印 LocalCache
        for (Object o : h.listCache()) {
            System.out.println(o);
        }
    }

}
