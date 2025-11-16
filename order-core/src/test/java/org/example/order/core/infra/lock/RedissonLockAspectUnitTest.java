//package org.example.order.core.infra.lock;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.order.core.infra.lock.config.LockCoreTestSlice;
//import org.example.order.core.infra.lock.config.RedissonMockEnabledConfig;
//import org.example.order.core.infra.lock.support.DummyLockTarget;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.aop.aspectj.AbstractAspectJAdvice;
//import org.springframework.aop.framework.Advised;
//import org.springframework.aop.support.AopUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Slf4j
//@SpringBootTest(classes = RedissonLockAspectUnitTest.Boot.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ActiveProfiles("test-unit")
//@TestPropertySource(properties = {
//        "lock.enabled=true",
//        "lock.redisson.enabled=true",
//        "lock.named.enabled=false",
//        "spring.autoconfigure.exclude=" +
//                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
//})
//@Import({
//        LockCoreTestSlice.class,
//        RedissonMockEnabledConfig.class
//})
//class RedissonLockAspectUnitTest {
//
//    @SpringBootConfiguration
//    @EnableAutoConfiguration
//    static class Boot {
//        @Bean
//        DummyLockTarget dummyLockTarget() {
//            return new DummyLockTarget();
//        }
//    }
//
//    private final DummyLockTarget target;
//
//    @Autowired
//    RedissonLockAspectUnitTest(DummyLockTarget target) {
//        this.target = target;
//    }
//
//    @BeforeEach
//    void setUp() {
//        target.reset();
//    }
//
//    @Test
//    void advisor_is_attached_on_proxy() {
//        assertThat(AopUtils.isAopProxy(target)).isTrue();
//
//        boolean hasAdvisor = false;
//        if (target instanceof Advised advised) {
//            hasAdvisor = java.util.Arrays.stream(advised.getAdvisors())
//                    .map(a -> a.getAdvice())
//                    .filter(AbstractAspectJAdvice.class::isInstance)
//                    .map(AbstractAspectJAdvice.class::cast)
//                    .anyMatch(adj ->
//                            // bean 메서드명이 aspect name 이 됨: distributedLockAspect
//                            String.valueOf(adj.getAspectName()).contains("distributedLockAspect")
//                                    || adj.toString().contains("DistributedLockAspect"));
//        }
//
//        assertThat(hasAdvisor)
//                .as("DistributedLock Aspect advisor attached")
//                .isTrue();
//    }
//
//    @Test
//    void concurrent_access_is_serialized() throws InterruptedException {
//        int threadCount = 5;
//        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            pool.submit(() -> {
//                try {
//                    target.criticalSection();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        pool.shutdown();
//
//        assertThat(target.getCounter()).isEqualTo(threadCount);
//        assertThat(target.getMaxObserved()).isEqualTo(1);
//    }
//}
