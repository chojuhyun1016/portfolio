// order-api/order-api-master/src/main/java/org/example/order/api/master/OrderApiMasterApplication.java
package org.example.order.api.master;

import org.example.order.api.master.config.OrderApiMasterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * OrderApiMasterApplication
 * ------------------------------------------------------------------------
 * 목적
 * - API Master 애플리케이션의 진입점
 * - SpringBootApplication + OrderApiMasterConfig Import
 */
@SpringBootApplication
@Import(OrderApiMasterConfig.class)
public class OrderApiMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApiMasterApplication.class, args);
    }
}
