package org.example.order.core.infra.security.gateway.config;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.gateway.filter.GatewayAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayRouteConfig {

    private final GatewayAuthenticationFilter jwtGatewayAuthenticationFilter;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("lb://auth-service"))

                .route("user-service", r -> r.path("/api/v1/users/**")
                        .filters(f -> f.filter(jwtGatewayAuthenticationFilter))
                        .uri("lb://user-service"))

                .route("order-service", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(jwtGatewayAuthenticationFilter))
                        .uri("lb://order-service"))

                .build();
    }
}
