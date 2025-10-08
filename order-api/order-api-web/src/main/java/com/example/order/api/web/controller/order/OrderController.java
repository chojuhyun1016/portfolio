package com.example.order.api.web.controller.order;

import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.logging.Correlate;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 목적
 * - 주문 조회 API
 * <p>
 * MDC/trace 전략 (권장, 실무 스타일)
 * - @Correlate(paths=...):
 * 우선순위(안정/관용 순): 경로변수(#p0) -> 쿼리스트링(#p1.getParameter('orderId'))
 * -> 헤더(X-Order-Id -> X-Request-Id -> x-request-id)
 * (GET에서는 경로변수가 1순위이며, 프록시/캐시 계층 등 특수 케이스 보강을 위해 쿼리/헤더를 후순위로 둔다)
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderFacade facade;

    @GetMapping("/{orderId}")
    @Correlate(
            paths = {
                    // 경로변수
                    "#p0",
                    // 쿼리스트링
                    "#p1?.getParameter('orderId')",
                    // 헤더 (관용적으로 쓰이는 3종 우선순위)
                    "#p1?.getHeader('X-Order-Id')",
                    "#p1?.getHeader('X-Request-Id')",
                    "#p1?.getHeader('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public ResponseEntity<ApiResponse<OrderResponse>> findById(
            @PathVariable Long orderId,
            HttpServletRequest httpReq
    ) {
        log.info("[OrderController][findById] orderId={}", orderId);

        return ApiResponse.ok(facade.findById(orderId));
    }
}
