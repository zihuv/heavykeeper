package com.zihuv;

import com.zihuv.topK.HeavyKeeper;

public class App {
    public static void main(String[] args) {
        HeavyKeeper heavyKeeper = new HeavyKeeper(10, 10, 5, 0.925, 0);
        heavyKeeper.add("cdf",4);
        heavyKeeper.add("abc",2);
        System.out.println(heavyKeeper.list());
        heavyKeeper.add("abc",2);
        heavyKeeper.add("abc",2);
        System.out.println(heavyKeeper.list());
        heavyKeeper.fading();
        System.out.println(heavyKeeper.list());
    }
}
