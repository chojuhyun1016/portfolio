package org.example.order.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisService {

    // Value
    void saveValue(String key, Object value, long ttlSeconds);
    Object getValue(String key);
    boolean deleteKey(String key);

    // Hash
    void saveHash(String hashKey, Map<Object, Object> values);
    Object getHashValue(String hashKey, Object key);
    List<Object> getAllHashValues(String hashKey);
    void deleteHashKey(String hashKey, Object key);

    // List
    void addToList(String listKey, Collection<Object> values);
    Object popLeftFromList(String listKey);
    List<Object> popRightMultiple(String listKey, int count);

    // Set
    void addSetMembers(String setKey, Collection<Object> members);
    Set<Object> getSetMembers(String setKey);
    boolean isMember(String setKey, Object value);

    // ZSet
    boolean addSortedSet(String key, Object value, double score);
    Set<Object> getSortedSetByScore(String key, double min, double max);
    Long removeSortedSetRange(String key, double min, double max);

    // TTL
    void setTTL(String key, long seconds);
    Long getTTL(String key);
    boolean persistKey(String key);

    // Keys
    Set<String> findKeys(String pattern);

    // Transaction
    void transactionalHashPut(String hashKey, Map<Object, Object> map);
    void transactionalSetAdd(String setKey, Collection<Object> members);
}
