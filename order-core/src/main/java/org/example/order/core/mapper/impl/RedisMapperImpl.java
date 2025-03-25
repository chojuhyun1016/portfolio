package org.example.order.core.mapper.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.mapper.RedisMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisMapperImpl<K, V> implements RedisMapper<K, V> {

    private final RedisTemplate<K, V> redisTemplate;

    // ===== Value =====
    @Override
    public void set(K key, V value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(K key, V value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public V get(K key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean delete(K key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // ===== Hash =====
    @Override
    public void putHash(K hashKey, Object key, Object value) {
        redisTemplate.opsForHash().put(hashKey, key, value);
    }

    @Override
    public Object getHash(K hashKey, Object key) {
        return redisTemplate.opsForHash().get(hashKey, key);
    }

    @Override
    public void putAllHash(K hashKey, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(hashKey, map);
    }

    @Override
    public void deleteHash(K hashKey, Object key) {
        redisTemplate.opsForHash().delete(hashKey, key);
    }

    @Override
    public List<Object> getHashValues(K hashKey) {
        return new ArrayList<>(redisTemplate.opsForHash().values(hashKey));
    }

    @Override
    public long getHashSize(K hashKey) {
        return redisTemplate.opsForHash().size(hashKey);
    }

    // ===== List =====
    @Override
    public Long leftPush(K key, V value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public Long leftPushAll(K key, Collection<V> values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    @Override
    public void rightPush(K key, V value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public V leftPop(K key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public V rightPop(K key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public List<V> rightPop(K key, int loop) {
        List<V> result = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            V item = redisTemplate.opsForList().rightPop(key);
            if (item == null) break;
            result.add(item);
        }
        return result;
    }

    @Override
    public Long listSize(K key) {
        return redisTemplate.opsForList().size(key);
    }

    // ===== Set =====
    @Override
    public void addSet(K key, V value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @Override
    public void addAllSet(K key, Collection<V> values) {
        redisTemplate.opsForSet().add(key, values.toArray((V[]) new Object[0]));
    }

    @Override
    public Set<V> getSetMembers(K key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public boolean isSetMember(K key, V value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public Long removeSet(K key, V value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    @Override
    public Long getSetSize(K key) {
        return redisTemplate.opsForSet().size(key);
    }

    // ===== ZSet =====
    @Override
    public boolean zAdd(K key, V value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    @Override
    public Set<V> zRangeByScore(K key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    @Override
    public Long zRemoveRangeByScore(K key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    @Override
    public Long zCard(K key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    @Override
    public Double zScore(K key, V value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Long zRemove(K key, V value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    // ===== TTL =====
    @Override
    public boolean expire(K key, long timeoutSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS));
    }

    @Override
    public Long getExpire(K key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public boolean persist(K key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    // ===== Keys =====
    @Override
    public Set<K> keys(K pattern) {
        return redisTemplate.keys(pattern);
    }

    // ===== Transaction =====
    @Override
    public List<Object> transactionLeftPushAll(K key, Collection<V> values) {
        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForList().leftPushAll(key, values);
                return operations.exec();
            }
        });
    }

    @Override
    public List<Object> transactionPutAllHash(K hashKey, Map<Object, Object> map) {
        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForHash().putAll(hashKey, map);
                return operations.exec();
            }
        });
    }

    @Override
    public List<Object> transactionAddSet(K key, Collection<V> values) {
        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForSet().add(key, values.toArray());
                return operations.exec();
            }
        });
    }
}
