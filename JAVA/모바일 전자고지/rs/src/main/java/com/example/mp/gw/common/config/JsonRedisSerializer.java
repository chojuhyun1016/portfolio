package com.example.mp.gw.common.config; 


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;


public class JsonRedisSerializer implements RedisSerializer<Object>
{
	final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectMapper om;

	public JsonRedisSerializer()
	{
		this.om = new ObjectMapper().activateDefaultTyping(new ObjectMapper().getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);
	}

	@Override
	public byte[] serialize(Object t) throws SerializationException
	{
		try
		{
			if (t instanceof byte[])
				return (byte[]) t;
			else
				return om.writeValueAsBytes(t);
		}
		catch (JsonProcessingException e)
		{
			throw new SerializationException(e.getMessage(), e);
		}
	}

	@Override
	public Object deserialize(byte[] bytes) throws SerializationException
	{
		if (null == bytes)
		{
			return null;
		}
		
		try
		{
			return om.readValue(bytes, Object.class);
		}
		catch (Exception e)
		{
			return bytes;
		}
	}
}
