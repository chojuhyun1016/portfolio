package org.example.order.core.infra;

import org.example.order.core.TestBootApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = TestBootApp.class,
        properties = "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
)
@ActiveProfiles("test-unit")
class ContextSmokeTest {

    @Test
    void contextLoads() {
    }
}
