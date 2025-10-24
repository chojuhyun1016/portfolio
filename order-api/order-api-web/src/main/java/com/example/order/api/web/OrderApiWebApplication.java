package com.example.order.api.web;

import com.example.order.api.web.config.OrderApiWebConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * OrderApiWebApplication
 * ------------------------------------------------------------------------
 * 목적
 * - API Web 애플리케이션의 진입점
 * - SpringBootApplication + OrderApiWebConfig Import
 */
@SpringBootApplication
@Import(OrderApiWebConfig.class)
public class OrderApiWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApiWebApplication.class, args);
    }
}
