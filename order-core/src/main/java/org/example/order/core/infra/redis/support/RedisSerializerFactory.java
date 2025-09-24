package org.example.order.core.infra.redis.support;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis JSON Serializer Factory
 * - JSR-310(JavaTime) 지원
 * - Default typing(신뢰 패키지 한정)
 * - 필드 가시성 ALL
 */
public class RedisSerializerFactory {

    public static RedisSerializer<Object> create(String trustedPackage) {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1) JavaTime 모듈 등록 + 타임스탬프 비활성화 (ISO-8601 문자열로 저장)
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // (선택) 환경에 설치된 추가 모듈 자동 등록
        // objectMapper.findAndRegisterModules();

        // 2) 기본 타이핑(신뢰 패키지 한정) — 보안/호환 절충
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(trustedPackage)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        // 3) 가시성: 필드 기반 직렬화 허용
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 4) Redis용 Generic Jackson Serializer 생성
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
