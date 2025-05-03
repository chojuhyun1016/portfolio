package org.example.order.core.infra.security.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 전역 게이트웨이 예외 처리 핸들러 (Spring 6.x 호환)
 */
@Slf4j
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    protected static final String LOG_PREFIX = "[Gateway Exception]";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatusCode statusCode;

        if (ex instanceof ResponseStatusException responseStatusException) {
            statusCode = responseStatusException.getStatusCode();
        } else {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error("{} Unhandled exception: {} (status={})", LOG_PREFIX, ex.getMessage(), statusCode, ex);
        exchange.getResponse().setStatusCode(statusCode);

        return exchange.getResponse().setComplete();
    }
}
