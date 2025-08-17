package org.example.order.core.infra.redis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.redis.props.RedisProperties;
import org.example.order.core.infra.redis.support.RedisSerializerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 공통 구성:
 * - RedisConnectionFactory 존재 시 RedisTemplate/Serializer 등록
 * - trustedPackage: spring.redis.trustedPackage (RedisProperties 기본값 보장)
 */
@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class RedisCommonConfig {

    private final RedisProperties props;

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        // RedisProperties 기본값으로 항상 유효
        String trusted = props.getTrustedPackage();

        var json = RedisSerializerFactory.create(trusted);
        var str = new StringRedisSerializer();

        template.setKeySerializer(str);
        template.setValueSerializer(json);
        template.setHashKeySerializer(str);
        template.setHashValueSerializer(json);

        template.afterPropertiesSet();
        return template;
    }
}
