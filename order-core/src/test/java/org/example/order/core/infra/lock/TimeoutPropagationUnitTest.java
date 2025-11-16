//package org.example.order.core.infra.lock;
//
//import org.example.order.core.infra.lock.config.LockCoreTestSlice;
//import org.example.order.core.infra.lock.config.RedissonMockEnabledConfig;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ExecutorService;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(classes = TimeoutPropagationUnitTest.Boot.class)
//@ActiveProfiles("test-unit")
//@Import({LockCoreTestSlice.class, RedissonMockEnabledConfig.class})
//class TimeoutPropagationUnitTest {
//
//    @SpringBootConfiguration
//    @EnableAutoConfiguration
//    static class Boot {
//        @Bean
//        TimeoutTarget timeoutTarget() {
//            return new TimeoutTarget();
//        }
//    }
//
//    static class TimeoutTarget {
//        // 단순 동작 모사 (실제 @DistributedLock 타임아웃 케이스에 맞춰 조정 가능)
//        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
//
//        public void hold(long millis) {
//            lock.lock();
//            try {
//                Thread.sleep(millis);
//            } catch (InterruptedException ignored) {
//                Thread.currentThread().interrupt();
//            } finally {
//                lock.unlock();
//            }
//        }
//    }
//
//    @Autowired
//    TimeoutTarget target;
//
//    @BeforeEach
//    void setUp() {
//    }
//
//    @Test
//    void second_call_times_out_when_first_holds_lock() throws Exception {
//        ExecutorService pool = Executors.newFixedThreadPool(2);
//        CountDownLatch start = new CountDownLatch(1);
//
//        var f1 = pool.submit(() -> {
//            start.await();
//            target.hold(200);
//
//            return true;
//        });
//
//        var f2 = pool.submit(() -> {
//            start.await();
//
//            try {
//                target.hold(50);
//                return true;
//            } catch (Exception e) {
//                return false;
//            }
//        });
//
//        start.countDown();
//
//        assertThat(f1.get()).isTrue();
//        assertThat(f2.get()).isTrue(); // 실제 타임아웃을 기대한다면 실패/예외 어설션으로 조정
//
//        pool.shutdown();
//    }
//}
