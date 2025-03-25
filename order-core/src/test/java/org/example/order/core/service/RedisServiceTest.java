package org.example.order.core.service;

import org.example.order.core.mapper.RedisMapper;
import org.example.order.core.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RedisServiceTest {

    private RedisMapper<String, Object> redisMapper;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // RedisMapper를 Mock 객체로 생성
        redisMapper = mock(RedisMapper.class);
        redisService = new RedisServiceImpl(redisMapper);
    }

    @Test
    void saveValue_shouldDelegateToMapper() {
        redisService.saveValue("myKey", "myValue", 300);

        verify(redisMapper, times(1)).set("myKey", "myValue", 300);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        when(redisMapper.get("myKey")).thenReturn("expectedValue");

        Object value = redisService.getValue("myKey");

        assertThat(value).isEqualTo("expectedValue");
    }

    @Test
    void saveHash_shouldStoreMapCorrectly() {
        Map<Object, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        redisService.saveHash("hashKey", map);

        verify(redisMapper).putAllHash("hashKey", map);
    }

    @Test
    void getSetMembers_shouldReturnSet() {
        Set<Object> fakeSet = Set.of("one", "two");

        when(redisMapper.getSetMembers("mySet")).thenReturn(fakeSet);

        Set<Object> result = redisService.getSetMembers("mySet");

        assertThat(result).contains("one", "two");
    }

    @Test
    void getSortedSet_shouldCallZRangeByScore() {
        Set<Object> mockSet = new LinkedHashSet<>(List.of("v1", "v2"));
        when(redisMapper.zRangeByScore("myzset", 0, 100)).thenReturn(mockSet);

        Set<Object> result = redisService.getSortedSetByScore("myzset", 0, 100);

        assertThat(result).containsExactly("v1", "v2");
    }
}
