package org.example.order.core.lock.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    String key();
    String type(); // namedLock, redissonLock
    String keyStrategy() default "spell"; // ✅ 추가: 키 전략
    long waitTime() default 5000;
    long leaseTime() default 10000;
}
