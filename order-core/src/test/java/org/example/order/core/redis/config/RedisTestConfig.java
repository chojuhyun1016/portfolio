package org.example.order.core.redis.config;

import org.example.order.core.redis.repository.RedisRepository;
import org.example.order.core.redis.repository.impl.RedisRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisTestConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String host = System.getProperty("test.redis.host", "localhost");
        int port = Integer.parseInt(System.getProperty("test.redis.port", "6379"));
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisRepository redisRepository(RedisTemplate<String, Object> redisTemplate) {
        return new RedisRepositoryImpl(redisTemplate);
    }
}
