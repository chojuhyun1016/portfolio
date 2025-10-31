package org.example.order.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class OrderBatchApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(OrderBatchApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
