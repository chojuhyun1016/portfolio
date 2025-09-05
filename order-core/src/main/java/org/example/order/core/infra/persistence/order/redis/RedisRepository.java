package org.example.order.core.infra.persistence.order.redis;

import java.util.*;

public interface RedisRepository {

    // === Value ===
    void set(String key, Object value);                            // 기본 set

    void set(String key, Object value, long ttlSeconds);           // set + TTL

    Object get(String key);                                        // get

    boolean delete(String key);                                    // delete

    // === Hash ===
    void putHash(String hashKey, Object field, Object value);      // hash put

    void putAllHash(String hashKey, Map<Object, Object> map);      // hash put all

    Object getHash(String hashKey, Object field);                  // hash get

    List<Object> getAllHashValues(String hashKey);                 // hash get all values

    void deleteHash(String hashKey, Object field);                 // hash delete

    // === List ===
    void leftPush(String key, Object value);                       // list left push

    void leftPushAll(String key, Collection<Object> values);       // list left push all

    Object leftPop(String key);                                    // list left pop

    Object rightPop(String key);                                   // list right pop

    List<Object> rightPop(String key, int loop);                   // list right pop loop

    default List<Object> rightPop(String key, RedisListPopOptions options) {
        if (options == null || options.getBlockingTimeoutSeconds() == null || options.getBlockingTimeoutSeconds() <= 0) {
            int loop = (options != null && options.getLoop() != null && options.getLoop() > 0) ? options.getLoop() : 1;

            return rightPop(key, loop);
        }

        return rightPop(key, 1);
    }

    Long listSize(String key);                                     // list size

    // === Set ===
    void addSet(String key, Object value);                         // set add

    void addAllSet(String key, Collection<Object> values);         // set add all

    Set<Object> getSetMembers(String key);                         // set members

    boolean isSetMember(String key, Object value);                 // set is member

    Long removeSet(String key, Object value);                      // set remove

    Long getSetSize(String key);                                   // set size

    // === ZSet ===
    boolean zAdd(String key, Object value, double score);          // zset add

    Set<Object> zRangeByScore(String key, double min, double max); // zset range by score

    Long zRemoveRangeByScore(String key, double min, double max);  // zset remove by score

    Long zCard(String key);                                        // zset count

    Double zScore(String key, Object value);                       // zset score

    Long zRemove(String key, Object value);                        // zset remove

    // === TTL / Keys ===
    boolean expire(String key, long ttlSeconds);                   // expire

    Long getExpire(String key);                                    // get expire

    boolean persist(String key);                                   // persist

    Set<String> keys(String pattern);                              // keys

    // === Transaction ===
    void transactionPutAllHash(String hashKey, Map<Object, Object> map);   // tx put all hash

    void transactionAddSet(String key, Collection<Object> members);        // tx add set
}
