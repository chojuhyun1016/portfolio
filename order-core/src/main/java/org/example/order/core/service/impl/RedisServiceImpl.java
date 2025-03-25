package org.example.order.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.core.mapper.RedisMapper;
import org.example.order.core.service.RedisService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisMapper<String, Object> redisMapper;

    // === Value ===
    @Override
    public void saveValue(String key, Object value, long ttlSeconds) {
        redisMapper.set(key, value, ttlSeconds);
    }

    @Override
    public Object getValue(String key) {
        return redisMapper.get(key);
    }

    @Override
    public boolean deleteKey(String key) {
        return redisMapper.delete(key);
    }

    // === Hash ===
    @Override
    public void saveHash(String hashKey, Map<Object, Object> values) {
        redisMapper.putAllHash(hashKey, values);
    }

    @Override
    public Object getHashValue(String hashKey, Object key) {
        return redisMapper.getHash(hashKey, key);
    }

    @Override
    public List<Object> getAllHashValues(String hashKey) {
        return redisMapper.getHashValues(hashKey);
    }

    @Override
    public void deleteHashKey(String hashKey, Object key) {
        redisMapper.deleteHash(hashKey, key);
    }

    // === List ===
    @Override
    public void addToList(String listKey, Collection<Object> values) {
        redisMapper.leftPushAll(listKey, values);
    }

    @Override
    public Object popLeftFromList(String listKey) {
        return redisMapper.leftPop(listKey);
    }

    @Override
    public List<Object> popRightMultiple(String listKey, int count) {
        return redisMapper.rightPop(listKey, count);
    }

    // === Set ===
    @Override
    public void addSetMembers(String setKey, Collection<Object> members) {
        redisMapper.addAllSet(setKey, members);
    }

    @Override
    public Set<Object> getSetMembers(String setKey) {
        return redisMapper.getSetMembers(setKey);
    }

    @Override
    public boolean isMember(String setKey, Object value) {
        return redisMapper.isSetMember(setKey, value);
    }

    // === ZSet ===
    @Override
    public boolean addSortedSet(String key, Object value, double score) {
        return redisMapper.zAdd(key, value, score);
    }

    @Override
    public Set<Object> getSortedSetByScore(String key, double min, double max) {
        return redisMapper.zRangeByScore(key, min, max);
    }

    @Override
    public Long removeSortedSetRange(String key, double min, double max) {
        return redisMapper.zRemoveRangeByScore(key, min, max);
    }

    // === TTL ===
    @Override
    public void setTTL(String key, long seconds) {
        redisMapper.expire(key, seconds);
    }

    @Override
    public Long getTTL(String key) {
        return redisMapper.getExpire(key);
    }

    @Override
    public boolean persistKey(String key) {
        return redisMapper.persist(key);
    }

    // === Keys ===
    @Override
    public Set<String> findKeys(String pattern) {
        return redisMapper.keys(pattern);
    }

    // === Transaction ===
    @Override
    public void transactionalHashPut(String hashKey, Map<Object, Object> map) {
        redisMapper.transactionPutAllHash(hashKey, map);
    }

    @Override
    public void transactionalSetAdd(String setKey, Collection<Object> members) {
        redisMapper.transactionAddSet(setKey, members);
    }
}
