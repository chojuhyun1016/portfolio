//package org.example.order.core.infra.lock.support;
//
//import org.example.order.core.infra.lock.lock.LockCallback;
//import org.example.order.core.infra.lock.lock.LockExecutor;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantLock;
//
//public class InMemoryRedissonLockExecutor implements LockExecutor {
//
//    private static final Map<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
//
//    private ReentrantLock lockFor(String key) {
//        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
//    }
//
//    @Override
//    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
//        ReentrantLock lock = lockFor(key);
//        boolean acquired = false;
//
//        try {
//            acquired = lock.tryLock(Math.max(0, waitTime), TimeUnit.MILLISECONDS);
//
//            if (!acquired) {
//                throw new IllegalStateException("Failed to acquire lock for key=" + key);
//            }
//
//            return callback.call();
//        } finally {
//            if (acquired && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//}
