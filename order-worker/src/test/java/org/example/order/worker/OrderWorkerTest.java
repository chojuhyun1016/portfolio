package org.example.order.worker;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.config.OrderCoreConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@EnableAutoConfiguration
@SpringBootTest(classes = {OrderCoreConfig.class})
@ActiveProfiles("local")
public class OrderWorkerTest {
}
