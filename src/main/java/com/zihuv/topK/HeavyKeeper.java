package com.zihuv.topK;

import com.google.common.hash.Hashing;
import com.zihuv.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
public class HeavyKeeper implements TopK {

    private final int LOOKUP_TABLE = 256;

    private Integer k;
    private Integer width;
    private Integer depth;
    private Double decay;
    private Double[] lookupTable;
    private int minCount;

    private Double r; // 随机数，鉴定是否大于衰减数，以此决定是否剔除元素
    private Bucket[][] buckets;
    private PriorityQueue<Node> minHeap;
    private int total;

    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.decay = decay;
        this.lookupTable = new Double[LOOKUP_TABLE];
        this.minCount = minCount;

        this.r = Math.random();
        this.buckets = new Bucket[depth][width];
        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(Node::getCount));

        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                this.buckets[i][j] = new Bucket(0, 0);
            }
        }
        for (int i = 0; i < LOOKUP_TABLE; i++) {
            this.lookupTable[i] = Math.pow(decay, i);
        }
    }

    @Override
    public Pair<String, Boolean> add(String key, int incr) {
        byte[] keyBytes = key.getBytes();
        int itemFingerprint = Math.abs(Hashing.murmur3_32_fixed().hashBytes(keyBytes).asInt());
        int maxCount = 0;

        for (int i = 0; i < buckets.length; i++) {
            Bucket[] row = buckets[i];

            int bucketNumber = Math.abs(Hashing.murmur3_32_fixed(i).hashBytes(keyBytes).asInt()) % width;
            int fingerprint = row[bucketNumber].fingerprint;
            int count = row[bucketNumber].count;

            // 该位置没有元素，直接将新元素添加进去
            if (count == 0) {
                row[bucketNumber].fingerprint = itemFingerprint;
                row[bucketNumber].count += incr;
                maxCount = Math.max(maxCount, incr);
            } else if (fingerprint == itemFingerprint) {
                // 该位置的元素就是自己，直接加上 incr
                row[bucketNumber].count += incr;
                maxCount = Math.max(maxCount, row[bucketNumber].count);
            } else {
                // 发生了 hash 冲突，需要衰减次数
                for (int localIncr = incr; localIncr > 0; localIncr--) {
                    int curCount = row[bucketNumber].count;
                    double decay = row[bucketNumber].count < LOOKUP_TABLE ? lookupTable[curCount] : lookupTable[LOOKUP_TABLE - 1];
                    // 当计数值越大，衰减概率越低。保证低频元素被快速剔除，让高频元素保留
                    if (r < decay) {
                        row[bucketNumber].count--;
                        // 原位置的元素次数，被减到了 0，就将新的元素替换掉该位置
                        if (row[bucketNumber].count == 0) {
                            row[bucketNumber].fingerprint = itemFingerprint;
                            row[bucketNumber].count = localIncr;
                            maxCount = Math.max(maxCount, localIncr);
                            break;
                        }
                    }
                }
            }
        }

        total += incr;
        // 如果新来的元素出现次数，比最小堆的顶部元素元素小，说明并不能被加入 topK 当中
        // 最低准入门槛
        if (maxCount < this.minCount) {
            return new Pair<>("", false);
        }
        // 当最小堆容量已经到达 k 时，判断是否能剔除掉最小元素
        int minHeapCount = minHeap.peek() == null ? 0 : minHeap.peek().getCount();
        if (minHeap.size() > k && maxCount < minHeapCount) {
            return new Pair<>("", false);
        }

        // 如果该元素在 topK 中，已经存在，那么修改该节点出现次数，并重构最小堆；不存在，直接添加
        Node targetNode = null;
        for (Node node : minHeap) {
            if (Objects.equals(node.getKey(), key)) {
                targetNode = node;
                break;
            }
        }

        if (targetNode != null) {
            targetNode.setCount(maxCount);
            minHeap.remove(targetNode);
            minHeap.add(targetNode);
            return new Pair<>(targetNode.getKey(), true);
        }

        targetNode = new Node(key, maxCount);
        minHeap.add(targetNode);
        return new Pair<>("", true);
    }

    @Override
    public List<Node> list() {
        return new ArrayList<>(minHeap);
    }

    @Override
    public synchronized void fading() {
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                bucket.setCount(bucket.getCount() >> 1);
            }
        }

        for (Node node : minHeap) {
            node.setCount(node.getCount() >> 1);
        }

        total = total >> 1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Bucket {
        private Integer fingerprint;
        private Integer count;
    }

}