package org.example.order.core.infra.lock.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    String key();
    String type(); // namedLock, redissonLock
    String keyStrategy() default "sha256"; // sha256, md5, spell, simple
    long waitTime() default 3000;
    long leaseTime() default 10000;
}
