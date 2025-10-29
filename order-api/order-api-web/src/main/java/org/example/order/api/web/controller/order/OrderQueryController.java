package org.example.order.api.web.controller.order;

import org.example.order.api.web.dto.order.OrderQueryRequest;
import org.example.order.api.web.dto.order.OrderQueryResponse;
import org.example.order.api.web.facade.order.OrderQueryFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.support.logging.Correlate;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * 목적
 * - 주문 조회 API (POST)
 * - 각각 MySQL, DynamoDB, Redis 를 조회하여 주문 정보를 반환
 * <p>
 * MDC/trace 전략 (권장, 실무 스타일)
 * - Body(#p0.orderId) -> 쿼리스트링(orderId) -> 헤더(X-Order-Id/X-Request-Id/x-request-id)
 * - 컨트롤러에서 @Correlate로 도메인 키(orderId)를 뽑아 traceId로 덮어씀
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderQueryController {

    private final OrderQueryFacade facade;

    @PostMapping(value = "/mysql/query", consumes = APPLICATION_JSON_VALUE)
    @Correlate(
            paths = {
                    "#p0?.orderId",
                    "#p1?.getParameter('orderId')",
                    "#p1?.getHeader('X-Order-Id')",
                    "#p1?.getHeader('X-Request-Id')",
                    "#p1?.getHeader('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public ResponseEntity<? extends ApiResponse<?>> findByMySql(
            @RequestBody @Valid OrderQueryRequest req,
            HttpServletRequest httpReq
    ) {
        if (req.orderId() == null) {
            log.warn("[OrderQueryController][findByMySql] missing orderId in request body");

            return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
        }

        log.info("[OrderQueryController][findByMySql] orderId={}", req.orderId());

        OrderQueryResponse res = facade.findByMySql(req.orderId());

        return ApiResponse.ok(res);
    }

    @PostMapping(value = "/dynamo/query", consumes = APPLICATION_JSON_VALUE)
    @Correlate(
            paths = {
                    "#p0?.orderId",
                    "#p1?.getParameter('orderId')",
                    "#p1?.getHeader('X-Order-Id')",
                    "#p1?.getHeader('X-Request-Id')",
                    "#p1?.getHeader('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public ResponseEntity<? extends ApiResponse<?>> findByDynamo(
            @RequestBody @Valid OrderQueryRequest req,
            HttpServletRequest httpReq
    ) {
        if (req.orderId() == null) {
            log.warn("[OrderQueryController][findByDynamo] missing orderId in request body");

            return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
        }

        log.info("[OrderQueryController][findByDynamo] orderId={}", req.orderId());

        OrderQueryResponse res = facade.findByDynamo(req.orderId());

        return ApiResponse.ok(res);
    }

    @PostMapping(value = "/redis/query", consumes = APPLICATION_JSON_VALUE)
    @Correlate(
            paths = {
                    "#p0?.orderId",
                    "#p1?.getParameter('orderId')",
                    "#p1?.getHeader('X-Order-Id')",
                    "#p1?.getHeader('X-Request-Id')",
                    "#p1?.getHeader('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public ResponseEntity<? extends ApiResponse<?>> findByRedis(
            @RequestBody @Valid OrderQueryRequest req,
            HttpServletRequest httpReq
    ) {
        if (req.orderId() == null) {
            log.warn("[OrderQueryController][findByRedis] missing orderId in request body");

            return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
        }

        log.info("[OrderQueryController][findByRedis] orderId={}", req.orderId());

        OrderQueryResponse res = facade.findByRedis(req.orderId());

        return ApiResponse.ok(res);
    }
}
