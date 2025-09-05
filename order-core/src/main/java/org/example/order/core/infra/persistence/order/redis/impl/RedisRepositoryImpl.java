package org.example.order.core.infra.persistence.order.redis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.persistence.order.redis.RedisRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisRepository 구현체 - 기본 Redis 연산
 * 등록은 설정(InfraConfig)에서 @Bean 으로만 수행 (컴포넌트 스캔 미사용)
 */
@RequiredArgsConstructor
@Slf4j
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // === Value ===
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // === Hash ===
    @Override
    public void putHash(String hashKey, Object field, Object value) {
        redisTemplate.opsForHash().put(hashKey, field, value);
    }

    @Override
    public void putAllHash(String hashKey, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(hashKey, map);
    }

    @Override
    public Object getHash(String hashKey, Object field) {
        return redisTemplate.opsForHash().get(hashKey, field);
    }

    @Override
    public List<Object> getAllHashValues(String hashKey) {
        return new ArrayList<>(redisTemplate.opsForHash().values(hashKey));
    }

    @Override
    public void deleteHash(String hashKey, Object field) {
        redisTemplate.opsForHash().delete(hashKey, field);
    }

    // === List ===
    @Override
    public void leftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public void leftPushAll(String key, Collection<Object> values) {
        redisTemplate.opsForList().leftPushAll(key, values);
    }

    @Override
    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Object rightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public List<Object> rightPop(String key, int loop) {
        List<Object> result = new ArrayList<>();

        for (int i = 0; i < loop; i++) {
            Object val = redisTemplate.opsForList().rightPop(key);

            if (val == null) {
                break;
            }
            result.add(val);
        }

        return result;
    }

    @Override
    public Long listSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // === Set ===
    @Override
    public void addSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @Override
    public void addAllSet(String key, Collection<Object> values) {
        redisTemplate.opsForSet().add(key, values.toArray());
    }

    @Override
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public boolean isSetMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public Long removeSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    @Override
    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // === ZSet ===
    @Override
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    @Override
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    @Override
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    @Override
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Long zRemove(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    // === TTL ===
    @Override
    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS));
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    // === Transaction ===
    @Override
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

    @Override
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

    // === TTL / Keys ===
    @Override
    public Set<String> keys(String pattern) {
        final String match = (pattern == null || pattern.isBlank()) ? "*" : pattern;

        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> results = new LinkedHashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(match).count(1000).build();

            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    results.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                log.warn("SCAN failed for pattern '{}': {}", match, e.toString());
            }

            return results;
        });
    }
}
