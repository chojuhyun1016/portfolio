package org.example.order.api.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.order")
public class OrderApiMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApiMasterApplication.class, args);
    }
}
