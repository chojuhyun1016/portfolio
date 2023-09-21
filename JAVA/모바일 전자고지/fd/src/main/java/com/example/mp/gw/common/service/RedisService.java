package com.example.mp.gw.common.service;


import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.example.mp.gw.common.exception.FailConnectToRedisException;
import com.example.mp.gw.common.mappers.RedisMapper;

/**
 * @Class Name : RedisService.java
 * @Description : 기본 레디스 서비스
 *
 * @author 조주현
 * @since 2021.04.23
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.23	    조주현          최초 생성
 *
 *      </pre>
 *
 */


@Service
public class RedisService<K,V>
{
	@Autowired
    public RedisConnectionFactory redisConnectionFactory; 

	@Autowired
	private RedisMapper<K,V> redisMapper;
	
	
	public void set(K key,V value)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		redisMapper.set(key,value);
	}

	public void set(K key,V value, long expireSec)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		redisMapper.set(key,value, expireSec);
	}

	public V get(K key)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.get(key);
	}

	public void hmset(K hashKey, K key,V value)
	{ 
		if(!isConnected())
			throw new FailConnectToRedisException();

		redisMapper.hmset(hashKey, key, value);
	}			

	@SuppressWarnings("unchecked")
	public V hmget(K hashKey, K key)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return (V) redisMapper.hmget(hashKey, key);
	}

	public void hmdel(K key,K hashKey)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		redisMapper.hmdel(key, hashKey);
	}

	public Long lpush(K key, V value)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.leftPush(key, value);
	}

	public Long lpush(K key, Collection<V> values)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.leftPush(key, values);
	}

	public void rpush(K key, V value)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		redisMapper.rightPush(key, value);
	}

	public V lpop(K key)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.lPop(key);
	}

	public V rpop(K key)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.rPop(key);
	}

	public List<Object> lpushT(K listkey, Collection<V> values)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.leftPushT(listkey, values);
	}
	
	public List<V> rPop(K key,int loop)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.rPop(key, loop);
	}
	
	public Long size(K key)
	{
		if(!isConnected())
			throw new FailConnectToRedisException();

		return redisMapper.size(key);
	}

    public Long size_h(K key)
    {
        if(!isConnected())
            throw new FailConnectToRedisException();

        return redisMapper.size_h(key);
    }

	public boolean isConnected()
	{
		return "PONG".equals(redisConnectionFactory.getConnection().ping());
	}
}
