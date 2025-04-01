package org.example.order.core.redis;

import org.example.order.core.redis.repository.RedisRepository;
import org.example.order.core.redis.repository.impl.RedisRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

class RedisRepositoryImplMockTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisRepository redisRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisRepository = new RedisRepositoryImpl(redisTemplate);
    }

    @Test
    void testSet() {
        redisRepository.set("key", "value");
        verify(valueOperations).set("key", "value");
    }

    @ParameterizedTest
    @ValueSource(longs = {60, 120, 300})
    void testSetWithTTL(long ttl) {
        redisRepository.set("key", "value", ttl);
        verify(valueOperations).set("key", "value", ttl, java.util.concurrent.TimeUnit.SECONDS);
    }
}
