package org.example.order.worker.main;

import jakarta.annotation.PostConstruct;
import org.example.order.worker.config.OrderWorkerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@Import(OrderWorkerConfig.class)
public class OrderWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderWorkerApplication.class);
    }

    @PostConstruct
    private void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
