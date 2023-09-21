package com.example.mp.gw.common.mappers;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Class Name : RedisMapper.java
 * @Description : Lettuce를 이용한 Redis 쿼리 수행 Mapper
 * 
 * @author 조주현
 * @since 2021. 3. 8.
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자             수정내용
 *  -----------     -------------    ---------------------------
 *  2021. 3. 8	    조주현             최초 생성
 * 
 *  </pre>
 * 
 */


@Repository
public class RedisMapper<K,V>
{
	@Autowired
	RedisTemplate<K, V> redisTemplate;

	@Autowired
	StringRedisTemplate stringRedisTemplate;

	public void multi()
	{
		redisTemplate.multi();
	}
	
	public void exec()
	{
		redisTemplate.exec();
	}

	/**
	 * 현재의 키값 들을 확인하는 명령어
	 * @param pattern : 확인하려는 패턴 (※ 전체 조회는 *)
	 * @return : Set (※ Hashset) 형태의 조회된 키 값들
	 */
	public Set<K> keys(K pattern)
	{
		return redisTemplate.keys(pattern);
	}

	/**
	 * 키에 해당하는 값을 가져오는 명령어
	 * @param key : 불려오는 값의 키
	 * @return : 불러진 값
	 */
	public V get(K key)
	{
		return redisTemplate.opsForValue().get(key);
	}

	/**
	 * 키/값을 저장하는 명령어
	 * @param key : 저장하려는 값의 키
	 * @param value : 저장하려는 값
	 */
	public void set(K key, V value)
	{
		redisTemplate.opsForValue().set(key, value);
	}

	/**
	 * 키/값을 저장하는 명령어
	 * @param key : 저장하려는 값의 키
	 * @param value : 저장하려는 값
	 * @param expireSec : 만료시각(초)
	 */
	public void set(K key, V value, long expireSec)
	{
		redisTemplate.opsForValue().set(key, value, expireSec, TimeUnit.SECONDS);
	}

	/**
	 * 키와 해당하는 값을 삭제하는 명령어
	 * @param key : 삭제하려는 값의 키
	 * @return : 결과 boolean 값
	 */
	public boolean delete(K key)
	{
		return redisTemplate.delete(key);
	}

	/**
	 * String 형식의 목록의 맨 끝에 값을 추가하는 명령어
	 * @param listkey : 추가하려는 목록의 키
	 * @param value : 추가하려는 값
	 */
	public void rightPush(K listkey, V value)
	{
		redisTemplate.opsForList().rightPush(listkey, value);
	}

	/**
	 * String 형식의 목록의 사이즈를 확인 
	 * @param listkey : 추가하려는 목록의 키
	 * @param value : 추가하려는 값
	 */
	public Long size(K listkey)
	{
		return redisTemplate.opsForList().size(listkey);
	}

	/**
	 * String 형식의 목록의 맨 앞에 값을 추가하는 명령어
	 * @param listkey : 추가하려는 목록의 키
	 * @param value : 추가하려는 값
	 */
	public Long leftPush(K listkey, V value)
	{
		return redisTemplate.opsForList().leftPush(listkey, value);
	}

	/**
	 * String 형식의 목록의 맨 앞에 값목록을 추가하는 명령어
	 * @param listkey : 추가하려는 목록의 키
	 * @param value : 추가하려는 값 목록
	 */
	public Long leftPush(K listkey, Collection<V> values)
	{
		return redisTemplate.opsForList().leftPushAll(listkey, values);
	}
	
	/**
	 * String 형식의 목록의 맨 앞에 값목록을 추가하는 명령어
	 * @param listkey : 추가하려는 목록의 키
	 * @param value : 추가하려는 값 목록
	 */
	public List<Object> leftPushT(K listkey, Collection<V> values)
	{
		return redisTemplate.execute(new SessionCallback<List<Object>>()
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public List<Object> execute(RedisOperations operations) throws DataAccessException
			{
				operations.multi();
				operations.opsForList().leftPushAll(listkey, values);

				return operations.exec();
			}
		});
	}
	
	/**
	 * String 형식의 목록의 건수를 뽑는 명령어
	 * @param listkey : 추가하려는 목록의 키
	 * @return : 목록의 건수
	 */
	public Integer getListSize(K listkey)
	{
		return redisTemplate.opsForList().size(listkey).intValue();
	}

	/**
	 * String 형식의 목록의 값들을 출력하는 명령어
	 * @param listkey : 출력하려는 목록의 키
	 * @param startIdx : 출력하려는 범위의 시작 인덱스 (※ 0부터 시작)
	 * @param endIdx : 출력하려는 범위의 끝 인덱스 (※ 전체를 출력하려면 -1)
	 * @return : 범위하는 목록
	 */
	public List<String> range(String listkey, int startIdx, int endIdx)
	{
		return null;
	}

	/**
	 * hashKey에 해당되는 키 값의 데이터를 가져오는 명령어
	 * @param hashKey : 가져오려는 해쉬의 키
	 * @param key : 가져오려는 값의 키
	 * @return 
	 */
	public Object hmget(K hashKey, K key)
	{
		return redisTemplate.opsForHash().get(hashKey, key);
	}

	/**
	 * 단건의 데이터를 hashKey에 해당되는 해쉬에 추가/저장하는 명령어
	 * @param hashKey : 추가/저장하려는 해쉬의 키
	 * @param key : 추가/저장하려는 값의 키
	 * @param value : 추가/저장하려는 값
	 */
	public void hmset(K hashKey, K key, V value)
	{
		redisTemplate.opsForHash().put(hashKey, key, value);
	}

	/**
	 * 단건의 데이터를 hashKey에 해당되는 해쉬에 추가/저장하는 명령어
	 * @param hashKey : 추가/저장하려는 해쉬의 키
	 * @param key : 추가/저장하려는 값의 키
	 * @param value : 추가/저장하려는 값
	 */
	public void hmset(String hashKey, String key, Object value) {
		stringRedisTemplate.opsForHash().put(hashKey, key, value);
	}

	/**
	 * map형식으로 이루어진 N개의 데이터를 hashKey에 해당되는 해쉬에 추가/저장하는 명령어
	 * @param hashKey : 추가/저장하려는 해쉬의 키
	 * @param map : 추가/저장하려는 맵
	 */
	@SuppressWarnings("unchecked")
	public void hmsetAll(K hashKey, V  map)
	{
		redisTemplate.opsForHash().putAll(hashKey, (Map<? extends Object, ? extends Object>) map);
	}

	/**
	 * hashKey에 해당되는 해쉬 값들의 목록을 출력하는 명령어
	 * @param hashKey : 출력하려는 해쉬의 키
	 * @return : 출력되는 목록
	 */
	@SuppressWarnings("unchecked")
	public List<V> getHashList(K hashKey)
	{
		return (List<V>) redisTemplate.opsForHash().values(hashKey);
	}

	/**
	 * hashKey에 해당되는 해쉬 값들의 목록의 건수를 출력하는 명령어
	 * @param hashKey : 출력하려는 해쉬의 키
	 * @return : 출력되는 목록의 건수
	 */
	public Integer getHashListSize(K hashKey)
	{
		return redisTemplate.opsForHash().size(hashKey).intValue();
	}

	/**
	 * key에 해당되는 리스트의 값들의 목록을 추출하는 명령어
	 * @param key : 추출하려는 리스트 키 
	 * @return : 출력되는 목록 
	 */
	public V lPop(K key)
	{
		return redisTemplate.execute(new SessionCallback<V>()
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public List<Object> execute(RedisOperations operations) throws DataAccessException
			{
				operations.multi();
				operations.opsForList().leftPop(key);

				return operations.exec();
			}
		});
	}

	/**
	 * key에 해당되는 리스트의 값들의 목록을 출력하는 명령어
	 * @param key : 추출하려는 리스트 키 
	 * @return : 출력되는 목록 
	 */
	public V rPop(K key)
	{
		return redisTemplate.opsForList().rightPop(key);
	}

	/**
	 * 키와 해시키를 이용하여 데이터를 삭제하는 명령어
	 * @param key : 삭제하려는 값의 키
	 * @param hashkeys : 삭제하려는 값의 해시키
	 */
	public void hmdel(K key, K hashKey)
	{
		redisTemplate.opsForHash().delete(key, hashKey);
	}

	/**
	 * @param key : 추출하려는 리스트 키 
	 * @param loop : 추출하려는 리스트의 최대 길이
	 * @return : 출력되는 목록 
	 */
	public List<V> rPop(K key,int loop)
	{
		List<V> results = new ArrayList<>();

		for(int i=0;i < loop;i++)
		{
			V result =redisTemplate.opsForList().rightPop(key);
			
			if(result == null)
				break;

			results.add(result);
		}

		return results;
	}

	/**
	 * key에 해당되는 초단위의 마감 시간 정보를 연장하는 명령어
	 * @param key : 연장하려는 값의 키
	 * @param expireSec : 만료시각(초)
	 */
	public boolean expire(String key, long expireSec)
	{
		return stringRedisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
	}
}
