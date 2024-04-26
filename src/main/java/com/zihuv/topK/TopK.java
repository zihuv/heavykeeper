package com.zihuv.topK;

import com.zihuv.util.Pair;

import java.util.List;

public interface TopK {

    /**
     * @return 被剔除的元素 key，是否添加元素成功
     */
    Pair<String, Boolean> add(String key, int incr);

    List<Node> list();

    void fading();

}