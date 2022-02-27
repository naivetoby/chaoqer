package com.chaoqer.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 通用随机数工具类
 *
 * @author toby
 */
public class RandomUtil {

    private final static Logger logger = LoggerFactory.getLogger(RandomUtil.class);

    /**
     * 双重校验锁获取一个Random单例
     *
     * @return
     */
    public static ThreadLocalRandom getRandom() {
        return ThreadLocalRandom.current();
    }

    /**
     * 获得一个[0,max)之间的整数。
     */
    public static int getRandomInt(int max) {
        return getRandom().nextInt(max);
    }

    /**
     * 获得一个[0,max)之间的整数。
     */
    public static long getRandomLong(long max) {
        return getRandom().nextLong(max);
    }

    /**
     * 从数组中随机获取一个元素
     */
    public static <E> E getRandomElement(E[] array) {
        return array[getRandomInt(array.length)];
    }

    /**
     * 从list中随机取得一个元素
     */
    public static <E> E getRandomElement(List<E> list) {
        return list.get(getRandomInt(list.size()));
    }

    /**
     * 从set中随机取得一个元素
     */
    public static <E> E getRandomElement(Set<E> set) {
        int rn = getRandomInt(set.size());
        int i = 0;
        for (E e : set) {
            if (i == rn) {
                return e;
            }
            i++;
        }
        return null;
    }

    /**
     * 从map中随机取得一个key
     */
    public static <K, V> K getRandomKeyFromMap(Map<K, V> map) {
        int rn = getRandomInt(map.size());
        int i = 0;
        for (K key : map.keySet()) {
            if (i == rn) {
                return key;
            }
            i++;
        }
        return null;
    }

    /**
     * 从map中随机取得一个value
     */
    public static <K, V> V getRandomValueFromMap(Map<K, V> map) {
        int rn = getRandomInt(map.size());
        int i = 0;
        for (V value : map.values()) {
            if (i == rn) {
                return value;
            }
            i++;
        }
        return null;
    }

    /**
     * 生成一个n位的随机数，用于验证码等
     */
    public static String getRandNumber(int n) {
        String rn = "";
        if (n > 0 && n < 10) {
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < n; i++) {
                str.append('9');
            }
            int num = Integer.parseInt(str.toString());
            while (rn.length() < n) {
                rn = String.valueOf(getRandomInt(num));
            }
        } else {
            rn = "0";
        }
        return rn;
    }

}