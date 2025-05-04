package org.example.order.core.infra.redis.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisRepository 구현체 - 기본적인 Redis 연산 모음
 */
@Component
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // === Value ===

    // 단일 값 설정
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 단일 값 설정 (TTL 포함)
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    // 단일 값 조회
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 단일 키 삭제
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // === Hash ===

    // Hash 필드 설정
    public void putHash(String hashKey, Object field, Object value) {
        redisTemplate.opsForHash().put(hashKey, field, value);
    }

    // Hash 여러 필드 설정
    public void putAllHash(String hashKey, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(hashKey, map);
    }

    // Hash 단일 필드 조회
    public Object getHash(String hashKey, Object field) {
        return redisTemplate.opsForHash().get(hashKey, field);
    }

    // Hash 전체 값 조회
    public List<Object> getAllHashValues(String hashKey) {
        return new ArrayList<>(redisTemplate.opsForHash().values(hashKey));
    }

    // Hash 필드 삭제
    public void deleteHash(String hashKey, Object field) {
        redisTemplate.opsForHash().delete(hashKey, field);
    }

    // === List ===

    // List 왼쪽 push
    public void leftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    // List 여러 개 왼쪽 push
    public void leftPushAll(String key, Collection<Object> values) {
        redisTemplate.opsForList().leftPushAll(key, values);
    }

    // List 왼쪽 pop
    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    // List 오른쪽 pop
    public Object rightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    // List 오른쪽 pop 반복
    public List<Object> rightPop(String key, int loop) {
        List<Object> result = new ArrayList<>();

        for (int i = 0; i < loop; i++) {
            Object val = redisTemplate.opsForList().rightPop(key);
            if (val == null) break;
            result.add(val);
        }

        return result;
    }

    // List 사이즈 조회
    public Long listSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // === Set ===

    // Set 추가
    public void addSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    // Set 여러 개 추가
    public void addAllSet(String key, Collection<Object> values) {
        redisTemplate.opsForSet().add(key, values.toArray());
    }

    // Set 전체 멤버 조회
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // Set 멤버 여부 확인
    public boolean isSetMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    // Set 멤버 삭제
    public Long removeSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    // Set 크기 조회
    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // === ZSet ===

    // ZSet 추가
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    // ZSet 범위 조회 (점수 기준)
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    // ZSet 점수 범위 삭제
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    // ZSet 전체 크기 조회
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    // ZSet 점수 조회
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    // ZSet 멤버 삭제
    public Long zRemove(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    // === TTL ===

    // 키 TTL 설정
    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS));
    }

    // TTL 조회 (초 단위)
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // 무제한 키로 변경
    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    // 키 패턴 조회
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // === Transaction ===

    // Hash 트랜잭션으로 putAll
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

    // Set 트랜잭션으로 add
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
