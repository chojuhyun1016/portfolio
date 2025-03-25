package org.example.order.core.mapper;

import java.util.*;

public interface RedisMapper<K, V> {
    // Value
    void set(K key, V value);
    void set(K key, V value, long timeoutSeconds);
    V get(K key);
    boolean delete(K key);

    // Hash
    void putHash(K hashKey, Object key, Object value);
    Object getHash(K hashKey, Object key);
    void putAllHash(K hashKey, Map<Object, Object> map);
    void deleteHash(K hashKey, Object key);
    List<Object> getHashValues(K hashKey);
    long getHashSize(K hashKey);

    // List
    Long leftPush(K key, V value);
    Long leftPushAll(K key, Collection<V> values);
    void rightPush(K key, V value);
    V leftPop(K key);
    V rightPop(K key);
    List<V> rightPop(K key, int loop);
    Long listSize(K key);

    // Set
    void addSet(K key, V value);
    void addAllSet(K key, Collection<V> values);
    Set<V> getSetMembers(K key);
    boolean isSetMember(K key, V value);
    Long removeSet(K key, V value);
    Long getSetSize(K key);

    // Sorted Set (ZSet)
    boolean zAdd(K key, V value, double score);
    Set<V> zRangeByScore(K key, double min, double max);
    Long zRemoveRangeByScore(K key, double min, double max);
    Long zCard(K key);
    Double zScore(K key, V value);
    Long zRemove(K key, V value);

    // TTL
    boolean expire(K key, long timeoutSeconds);
    Long getExpire(K key);
    boolean persist(K key);

    // Key Pattern
    Set<K> keys(K pattern);

    // Transactions
    List<Object> transactionLeftPushAll(K key, Collection<V> values);
    List<Object> transactionPutAllHash(K hashKey, Map<Object, Object> map);
    List<Object> transactionAddSet(K key, Collection<V> values);
}
