//package org.example.order.core.infra.lock;
//
//import org.example.order.core.infra.lock.factory.LockExecutorFactory;
//import org.example.order.core.infra.lock.lock.LockCallback;
//import org.example.order.core.infra.lock.lock.LockExecutor;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.*;
//
//class LockExecutorFactoryUnitTest {
//
//    static class DummyExec implements LockExecutor {
//        @Override
//        public Object execute(String key, long wait, long lease, LockCallback cb) throws Throwable {
//            return cb.call();
//        }
//    }
//
//    @Test
//    void getExecutor_success_and_unknown_type() {
//        var factory = new LockExecutorFactory(Map.of(
//                "namedLock", new DummyExec(),
//                "redissonLock", new DummyExec()
//        ));
//
//        assertThat(factory.getExecutor("namedLock")).isNotNull();
//        assertThat(factory.getExecutor("redissonLock")).isNotNull();
//
//        assertThatThrownBy(() -> factory.getExecutor("unknown"))
//                .isInstanceOf(IllegalArgumentException.class);
//    }
//}
