package org.example.order.core.infra.redis.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // === Value ===
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // === Hash ===
    public void putHash(String hashKey, Object field, Object value) {
        redisTemplate.opsForHash().put(hashKey, field, value);
    }

    public void putAllHash(String hashKey, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(hashKey, map);
    }

    public Object getHash(String hashKey, Object field) {
        return redisTemplate.opsForHash().get(hashKey, field);
    }

    public List<Object> getAllHashValues(String hashKey) {
        return new ArrayList<>(redisTemplate.opsForHash().values(hashKey));
    }

    public void deleteHash(String hashKey, Object field) {
        redisTemplate.opsForHash().delete(hashKey, field);
    }

    // === List ===
    public void leftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public void leftPushAll(String key, Collection<Object> values) {
        redisTemplate.opsForList().leftPushAll(key, values);
    }

    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public Object rightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public List<Object> rightPop(String key, int loop) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            Object val = redisTemplate.opsForList().rightPop(key);
            if (val == null) break;
            result.add(val);
        }
        return result;
    }

    public Long listSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // === Set ===
    public void addSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public void addAllSet(String key, Collection<Object> values) {
        redisTemplate.opsForSet().add(key, values.toArray());
    }

    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public boolean isSetMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public Long removeSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // === ZSet ===
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public Long zRemove(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    // === TTL ===
    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS));
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // === Transaction ===
    public void transactionPutAllHash(String hashKey, Map<Object, Object> map) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForHash().putAll(hashKey, map);
                return operations.exec();
            }
        });
    }

    public void transactionAddSet(String key, Collection<Object> members) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForSet().add(key, members.toArray());
                return operations.exec();
            }
        });
    }
}
