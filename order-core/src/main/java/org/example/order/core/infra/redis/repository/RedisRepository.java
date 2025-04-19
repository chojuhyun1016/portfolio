package org.example.order.core.infra.redis.repository;

import java.util.*;

public interface RedisRepository {
    // === Value ===
    void set(String key, Object value);
    void set(String key, Object value, long ttlSeconds);
    Object get(String key);
    boolean delete(String key);

    // === Hash ===
    void putHash(String hashKey, Object field, Object value);
    void putAllHash(String hashKey, Map<Object, Object> map);
    Object getHash(String hashKey, Object field);
    List<Object> getAllHashValues(String hashKey);
    void deleteHash(String hashKey, Object field);

    // === List ===
    void leftPush(String key, Object value);
    void leftPushAll(String key, Collection<Object> values);
    Object leftPop(String key);
    Object rightPop(String key);
    List<Object> rightPop(String key, int loop);
    Long listSize(String key);

    // === Set ===
    void addSet(String key, Object value);
    void addAllSet(String key, Collection<Object> values);
    Set<Object> getSetMembers(String key);
    boolean isSetMember(String key, Object value);
    Long removeSet(String key, Object value);
    Long getSetSize(String key);

    // === ZSet ===
    boolean zAdd(String key, Object value, double score);
    Set<Object> zRangeByScore(String key, double min, double max);
    Long zRemoveRangeByScore(String key, double min, double max);
    Long zCard(String key);
    Double zScore(String key, Object value);
    Long zRemove(String key, Object value);

    // === TTL / Keys ===
    boolean expire(String key, long ttlSeconds);
    Long getExpire(String key);
    boolean persist(String key);
    Set<String> keys(String pattern);

    // === Transaction ===
    void transactionPutAllHash(String hashKey, Map<Object, Object> map);
    void transactionAddSet(String key, Collection<Object> members);
}
