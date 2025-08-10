package com.example.order.api.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.example.order")
public class OrderApiWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApiWebApplication.class, args);
    }
}
