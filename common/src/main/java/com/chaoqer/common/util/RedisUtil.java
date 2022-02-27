package com.chaoqer.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.*;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redis调用工具类
 *
 * @author toby
 */
public class RedisUtil {

    public static String getString(StringRedisTemplate redisTemplate, String key) {
        return getObject(redisTemplate, key, String.class);
    }

    public static JSONObject getJSONObject(StringRedisTemplate redisTemplate, String key) {
        return getObject(redisTemplate, key, JSONObject.class);
    }

    public static JSONArray getJSONArray(StringRedisTemplate redisTemplate, String key) {
        return getObject(redisTemplate, key, JSONArray.class);
    }

    public static <T> T getObject(StringRedisTemplate redisTemplate, String key, Class<T> t) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        if (StringUtils.isNotBlank(key)) {
            String value = ops.get(key);
            if (StringUtils.isNotBlank(value)) {
                return JSON.parseObject(value, t);
            }
        }
        return null;
    }

    public static void setObject(StringRedisTemplate redisTemplate, String key, Object object) {
        setObject(redisTemplate, key, object, 0);
    }

    public static void setObject(StringRedisTemplate redisTemplate, String key, Object obj, long seconds) {
        setObject(redisTemplate, key, obj, seconds, TimeUnit.SECONDS);
    }

    public static void setObject(StringRedisTemplate redisTemplate, String key, Object obj, long timeout, TimeUnit unit) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        if (StringUtils.isNotBlank(key) && obj != null) {
            String value = JSON.toJSONString(obj);
            if (StringUtils.isNotBlank(value)) {
                if (timeout == 0) {
                    ops.set(key, value);
                } else {
                    ops.set(key, value, timeout, unit);
                }
            }
        }
    }

    public static boolean setLockKeyIfAbsent(StringRedisTemplate redisTemplate, String key, long seconds) {
        return setLockKeyIfAbsent(redisTemplate, key, seconds, TimeUnit.SECONDS);
    }

    public static boolean setLockKeyIfAbsent(StringRedisTemplate redisTemplate, String key, long timeout, TimeUnit unit) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        if (StringUtils.isNotBlank(key)) {
            if (timeout > 0) {
                Boolean result = ops.setIfAbsent(key, key, timeout, unit);
                return result != null && result;
            }
        }
        return false;
    }

    public static void setExpireTime(StringRedisTemplate redisTemplate, String key, long timeout) {
        if (timeout > 0) {
            setExpireTime(redisTemplate, key, timeout, TimeUnit.SECONDS);
        }
    }

    public static void setExpireTime(StringRedisTemplate redisTemplate, String key, long timeout, TimeUnit unit) {
        if (StringUtils.isNotBlank(key) && isKeyExist(redisTemplate, key)) {
            redisTemplate.expire(key, timeout, unit);
        }
    }

    public static void increment(StringRedisTemplate redisTemplate, String key) {
        increment(redisTemplate, key, -1);
    }

    public static void increment(StringRedisTemplate redisTemplate, String key, long delta) {
        if (StringUtils.isNotBlank(key)) {
            if (delta > -1) {
                // 可能在初始化
                redisTemplate.opsForValue().increment(key, delta);
            } else {
                // 需要存在时才累加
                if (isKeyExist(redisTemplate, key)) {
                    redisTemplate.opsForValue().increment(key);
                }
            }
        }
    }

    public static void decrement(StringRedisTemplate redisTemplate, String key) {
        if (StringUtils.isNotBlank(key) && isKeyExist(redisTemplate, key)) {
            redisTemplate.opsForValue().decrement(key);
        }
    }

    public static void sadd(StringRedisTemplate redisTemplate, String key, String value) {
        BoundSetOperations<String, String> ops = redisTemplate.boundSetOps(key);
        if (StringUtils.isNotBlank(value)) {
            ops.add(value);
        }
    }

    public static void sdel(StringRedisTemplate redisTemplate, String key, String value) {
        BoundSetOperations<String, String> ops = redisTemplate.boundSetOps(key);
        if (StringUtils.isNotBlank(value)) {
            ops.remove(value);
        }
    }

    public static String sgetByRand(StringRedisTemplate redisTemplate, String key) {
        BoundSetOperations<String, String> ops = redisTemplate.boundSetOps(key);
        return ops.randomMember();
    }

    public static Long scount(StringRedisTemplate redisTemplate, String key) {
        BoundSetOperations<String, String> ops = redisTemplate.boundSetOps(key);
        return ops.size();
    }

    public static void delObject(StringRedisTemplate redisTemplate, String key) {
        if (StringUtils.isNotBlank(key)) {
            redisTemplate.delete(key);
        }
    }

    public static boolean isKeyExist(StringRedisTemplate redisTemplate, String key) {
        if (StringUtils.isNotBlank(key)) {
            return CommonUtil.nullToDefault(redisTemplate.hasKey(key));
        }
        return false;
    }

    public static void hset(StringRedisTemplate redisTemplate, String key, String field, String value) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(field) && StringUtils.isNotBlank(value)) {
            ops.put(key, field, value);
        }
    }

    public static String hget(StringRedisTemplate redisTemplate, String key, String field) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(field)) {
            Object value = ops.get(key, field);
            return value == null ? "" : value.toString();
        }
        return "";
    }

    public static void hdel(StringRedisTemplate redisTemplate, String key, String field) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(field)) {
            ops.delete(key, field);
        }
    }

    public static Map<String, String> hgetall(StringRedisTemplate redisTemplate, String key) {
        if (StringUtils.isBlank(key)) {
            return new HashMap<>(0);
        }
        return redisTemplate.execute((RedisCallback<Map<String, String>>) con -> {
            Map<byte[], byte[]> result = con.hGetAll(key.getBytes());
            if (CollectionUtils.isEmpty(result)) {
                return new HashMap<>(0);
            }
            Map<String, String> ans = new HashMap<>(result.size());
            for (Map.Entry<byte[], byte[]> entry : result.entrySet()) {
                ans.put(new String(entry.getKey()), new String(entry.getValue()));
            }
            return ans;
        });
    }

}
