package com.speakmaster.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具�?
 * 封装常用的Redis操作
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ========== Key操作 ==========

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long time, TimeUnit unit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            log.error("设置过期时间失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取过期时间
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -1;
    }

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("判断key是否存在失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 删除key
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("删除key失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 批量删除key
     */
    public long delete(Collection<String> keys) {
        try {
            Long result = redisTemplate.delete(keys);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("批量删除key失败", e);
            return 0;
        }
    }

    // ========== String操作 ==========

    /**
     * 获取�?
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置�?
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置值失�? key={}", key, e);
            return false;
        }
    }

    /**
     * 设置值并设置过期时间
     */
    public boolean set(String key, Object value, long time, TimeUnit unit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, unit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置值失�? key={}", key, e);
            return false;
        }
    }

    /**
     * 递增
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * 递减
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().decrement(key, delta);
        return result != null ? result : 0;
    }

    // ========== Hash操作 ==========

    /**
     * 获取Hash中的�?
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取Hash中的所有键值对
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 设置Hash
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("设置Hash失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置Hash并设置过期时�?
     */
    public boolean hmset(String key, Map<String, Object> map, long time, TimeUnit unit) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            log.error("设置Hash失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 向Hash中放入数�?
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("向Hash中放入数据失�? key={}, item={}", key, item, e);
            return false;
        }
    }

    /**
     * 删除Hash中的�?
     */
    public long hdel(String key, Object... items) {
        return redisTemplate.opsForHash().delete(key, items);
    }

    /**
     * 判断Hash中是否有该项的�?
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    // ========== Set操作 ==========

    /**
     * 获取Set中的所有�?
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取Set失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 判断值是否在Set�?
     */
    public boolean sHasKey(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
            return result != null && result;
        } catch (Exception e) {
            log.error("判断值是否在Set中失�? key={}", key, e);
            return false;
        }
    }

    /**
     * 将数据放入Set
     */
    public long sSet(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().add(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("将数据放入Set失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 移除Set中的�?
     */
    public long sRemove(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("移除Set中的值失�? key={}", key, e);
            return 0;
        }
    }

    // ========== List操作 ==========

    /**
     * 获取List中的内容
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取List失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取List的长�?
     */
    public long lGetListSize(String key) {
        try {
            Long result = redisTemplate.opsForList().size(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取List长度失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 通过索引获取List中的�?
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("通过索引获取List中的值失�? key={}, index={}", key, index, e);
            return null;
        }
    }

    /**
     * 将值放入List
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("将值放入List失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 将List放入缓存
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("将List放入缓存失败: key={}", key, e);
            return false;
        }
    }

    // ========== ZSet操作 ==========

    /**
     * 添加到有序集�?
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            return result != null && result;
        } catch (Exception e) {
            log.error("添加到有序集合失�? key={}", key, e);
            return false;
        }
    }

    /**
     * 获取有序集合的成员数
     */
    public long zCard(String key) {
        try {
            Long result = redisTemplate.opsForZSet().zCard(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("获取有序集合的成员数失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 获取有序集合指定范围内的成员 (按分数从小到�?
     */
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            log.error("获取有序集合指定范围内的成员失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取有序集合指定范围内的成员 (按分数从大到�?
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("获取有序集合指定范围内的成员失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取成员的排�?(从小到大)
     */
    public long zRank(String key, Object value) {
        try {
            Long result = redisTemplate.opsForZSet().rank(key, value);
            return result != null ? result : -1;
        } catch (Exception e) {
            log.error("获取成员的排名失�? key={}", key, e);
            return -1;
        }
    }

    /**
     * 获取成员的排�?(从大到小)
     */
    public long zReverseRank(String key, Object value) {
        try {
            Long result = redisTemplate.opsForZSet().reverseRank(key, value);
            return result != null ? result : -1;
        } catch (Exception e) {
            log.error("获取成员的排名失�? key={}", key, e);
            return -1;
        }
    }

    /**
     * 移除有序集合中的成员
     */
    public long zRemove(String key, Object... values) {
        try {
            Long result = redisTemplate.opsForZSet().remove(key, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("移除有序集合中的成员失败: key={}", key, e);
            return 0;
        }
    }

    // ========== HyperLogLog操作 ==========

    /**
     * HyperLogLog添加
     */
    @SuppressWarnings("unchecked")
    public long pfAdd(String key, String... values) {
        try {
            Long result = redisTemplate.opsForHyperLogLog().add(key, (Object[]) values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("HyperLogLog添加失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * HyperLogLog计数
     */
    public long pfCount(String... keys) {
        try {
            Long result = redisTemplate.opsForHyperLogLog().size(keys);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("HyperLogLog计数失败", e);
            return 0;
        }
    }

    // ========== 便捷方法 ==========

    /**
     * 递增（简化版�?
     */
    public long increment(String key) {
        return incr(key, 1);
    }

    /**
     * 递减（简化版�?
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("递减失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取值并转换类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) return null;
        if (clazz.isInstance(value)) return (T) value;
        // 字符串转�?
        if (clazz == Long.class) return (T) Long.valueOf(value.toString());
        if (clazz == Integer.class) return (T) Integer.valueOf(value.toString());
        return (T) value;
    }

    /**
     * 设置值并设置过期时间（秒�?
     */
    public boolean set(String key, Object value, long seconds) {
        return set(key, value, seconds, TimeUnit.SECONDS);
    }
}
