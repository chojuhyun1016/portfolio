package org.example.order.worker.main;

import jakarta.annotation.PostConstruct;
import org.example.order.core.config.FlywayDevLocalStrategy;
import org.example.order.worker.config.OrderWorkerConfig;
import org.example.order.core.infra.config.OrderCoreConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@Import({
        OrderWorkerConfig.class,
        FlywayDevLocalStrategy.class,
        OrderCoreConfig.class
})
public class OrderWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderWorkerApplication.class, args);
    }

    @PostConstruct
    private void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
