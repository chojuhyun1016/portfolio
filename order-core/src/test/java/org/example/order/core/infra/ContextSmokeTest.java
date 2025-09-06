package org.example.order.core.infra;

import org.example.order.core.TestBoot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = TestBoot.class
)
@ActiveProfiles("test-unit")
class ContextSmokeTest {

    @Test
    void contextLoads() {
    }
}
