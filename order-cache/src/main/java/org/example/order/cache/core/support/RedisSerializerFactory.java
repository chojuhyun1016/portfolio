package org.example.order.cache.core.support;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis JSON Serializer Factory
 * - JSR-310(JavaTime) 지원
 * - Default typing(WRAPPER_ARRAY) 비활성화: 순수 JSON 객체와 호환
 * - 필드 가시성 ALL
 */
public class RedisSerializerFactory {

    public static RedisSerializer<Object> create(String ignoredTrustedPackage) {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1) 시간 모듈 + ISO-8601 문자열
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2) 기본 타이핑 활성화하지 않음(WRAPPER_ARRAY 제거)
        // objectMapper.activateDefaultTyping(...);

        // 3) 필드 기반 직렬화 허용
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 4) 래핑 없는 GenericJackson2JsonRedisSerializer
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
