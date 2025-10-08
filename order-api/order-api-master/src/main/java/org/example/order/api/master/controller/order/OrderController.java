package org.example.order.api.master.controller.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.LocalOrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.support.logging.Correlate;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 목적
 * - 주문 메시지를 수신하여 내부로 전달한다.
 * <p>
 * MDC/trace 전략 (권장, 실무 스타일)
 * 1) 요청 초입에서는 CorrelationIdFilter가 requestId를 생성/브리지(MDC["traceId"]=requestId)한다.
 * 2) 컨트롤러 메서드에서 @Correlate로 도메인 키(orderId)를 추출해 traceId를 덮어쓴다.
 * - 우선순위(안정/관용 순): 바디(#p0.orderId) -> 쿼리스트링(#p1.getParameter('orderId'))
 * -> 헤더(X-Order-Id -> X-Request-Id -> x-request-id)
 * - 바디가 없는 케이스나 프록시/게이트웨이 경유 환경에서도 최대한 복원 가능.
 * 3) 애플리케이션 코드에서 MDC.clear()를 호출하지 않는다(필터가 요청 종료 시점에 복원 처리).
 * <p>
 * 로그 패턴 예시 (logback):
 * [%d{yyyy-MM-dd HH:mm:ss}:%-3relative][%thread][%level][traceId:%X{traceId:-NA}][line:%L][%logger][%M] : %msg%n
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderFacade facade;

    /**
     * 주문 메시지를 수신하여 내부로 전달한다.
     *
     * @Correlate: 도메인 키(orderId) 추출 우선순위 (POST 실무 관용 패턴)
     * - 바디:        "#p0?.orderId"
     * - 쿼리스트링:  "#p1?.getParameter('orderId')"
     * - 헤더:        "#p1?.getHeader('X-Order-Id')" -> "#p1?.getHeader('X-Request-Id')" -> "#p1?.getHeader('x-request-id')"
     * - overrideTraceId=true: traceId를 도메인 키로 덮어씀
     * - mdcKey="orderId": 보조키 저장
     */
    @PostMapping
    @Correlate(
            paths = {
                    // 바디
                    "#p0?.orderId",
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
    public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
            @RequestBody @Valid LocalOrderRequest request,
            HttpServletRequest httpReq
    ) {
        log.info("[OrderController][sendOrderMasterMessage] orderId={}, methodType={}",
                request.orderId(), request.methodType());

        facade.sendOrderMessage(request);

        return ApiResponse.accepted(new LocalOrderResponse(request.orderId(), HttpStatus.ACCEPTED.name()));
    }
}
