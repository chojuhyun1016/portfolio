package org.example.order.core.redis.repository;

import org.example.order.core.infra.redis.repository.RedisRepository;
import org.example.order.core.redis.config.RedisTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = RedisTestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisRepositoryImplTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0.5")
            .withExposedPorts(6379);

    static {
        redisContainer.start();
        System.setProperty("test.redis.host", redisContainer.getHost());
        System.setProperty("test.redis.port", String.valueOf(redisContainer.getMappedPort(6379)));
    }

    @Autowired
    RedisRepository redisRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testSetAndGetValue() {
        redisRepository.set("key1", "value1");
        Object value = redisRepository.get("key1");
        assertThat(value).isEqualTo("value1");
    }

    @Test
    void testSetWithTTL() throws InterruptedException {
        redisRepository.set("tempKey", "tempValue", 1);
        assertThat(redisRepository.get("tempKey")).isEqualTo("tempValue");
        Thread.sleep(1500);
        assertThat(redisRepository.get("tempKey")).isNull();
    }

    @Test
    void testPersist() {
        redisRepository.set("persistKey", "persistValue", 10);
        assertThat(redisRepository.getExpire("persistKey")).isGreaterThan(0);
        redisRepository.persist("persistKey");
        assertThat(redisRepository.getExpire("persistKey")).isEqualTo(-1);
    }

    @Test
    void testDelete() {
        redisRepository.set("deleteKey", "toBeDeleted");
        assertThat(redisRepository.delete("deleteKey")).isTrue();
        assertThat(redisRepository.get("deleteKey")).isNull();
    }

    @Test
    void testHashOperations() {
        redisRepository.putHash("hashKey", "field1", "val1");
        Object val = redisRepository.getHash("hashKey", "field1");
        assertThat(val).isEqualTo("val1");
    }

    @Test
    void testHashPutAllAndGetAll() {
        Map<Object, Object> map = Map.of("a", 1, "b", 2);
        redisRepository.putAllHash("hashMap", map);
        List<Object> values = redisRepository.getAllHashValues("hashMap");
        assertThat(values).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void testListOperations() {
        redisRepository.leftPush("listKey", "A");
        redisRepository.leftPush("listKey", "B");
        List<Object> popped = redisRepository.rightPop("listKey", 2);
        assertThat(popped).containsExactly("A", "B");
    }

    @Test
    void testSetOperations() {
        redisRepository.addSet("setKey", "one");
        redisRepository.addSet("setKey", "two");
        Set<Object> members = redisRepository.getSetMembers("setKey");
        assertThat(members).contains("one", "two");
    }

    @Test
    void testZSetOperations() {
        redisRepository.zAdd("zKey", "user1", 10);
        redisRepository.zAdd("zKey", "user2", 20);
        Set<Object> range = redisRepository.zRangeByScore("zKey", 5, 15);
        assertThat(range).contains("user1");
    }

    @Test
    void testZSetRemoveAndCardinality() {
        redisRepository.zAdd("zKey2", "x", 1);
        redisRepository.zAdd("zKey2", "y", 2);
        assertThat(redisRepository.zCard("zKey2")).isEqualTo(2);
        redisRepository.zRemove("zKey2", "x");
        assertThat(redisRepository.zCard("zKey2")).isEqualTo(1);
    }

    @Test
    void testExpireAndTTL() throws InterruptedException {
        redisRepository.set("ttlKey", "123", 2);
        assertThat(redisRepository.getExpire("ttlKey")).isGreaterThan(0);
        Thread.sleep(2500);
        assertThat(redisRepository.get("ttlKey")).isNull();
    }

    @Test
    void testTransactionPutAllHash() {
        Map<Object, Object> txMap = Map.of("k1", "v1", "k2", "v2");
        redisRepository.transactionPutAllHash("txHash", txMap);
        assertThat(redisRepository.getHash("txHash", "k1")).isEqualTo("v1");
    }

    @Test
    void testTransactionAddSet() {
        List<Object> members = List.of("a", "b", "c");
        redisRepository.transactionAddSet("txSet", members);
        assertThat(redisRepository.getSetMembers("txSet")).containsAll(members);
    }

    @Test
    void testKeysPattern() {
        redisRepository.set("key:1", "v1");
        redisRepository.set("key:2", "v2");
        Set<String> keys = redisRepository.keys("key:*");
        assertThat(keys).contains("key:1", "key:2");
    }
}
