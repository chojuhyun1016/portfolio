package org.example.order.api.master.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.LocalOrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.web.response.ApiResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderFacade facade;

    /**
     * 주문 메시지를 수신하여 내부로 전달한다.
     * <p>
     * MDC(traceId) 사용 전략:
     * 1) 요청 시작 시 항상 UUID 기반 traceId를 먼저 세팅한다.
     * - 이유: 검증 전에라도 로그 추적이 바로 가능하도록 보장하기 위해서다.
     * 2) 요청 본문 검증이 끝난 뒤, 메시지 추적에 더 적합한 키(여기서는 orderId)가 있으면
     * traceId 값을 그 키로 교체한다. (도메인 친화적 키로 재설정)
     * 3) 처리 완료 후에는 반드시 MDC.clear()로 정리한다. (ThreadLocal 누수 방지)
     * <p>
     * 로그 패턴 예시 (logback):
     * [%d{yyyy-MM-dd HH:mm:ss}:%-3relative][%thread][%level][traceId:%X{traceId:-NA}][line:%L][%logger][%M] : %msg%n
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
            @RequestBody @Valid LocalOrderRequest request
    ) {
        // 1) 항상 기본 traceId를 먼저 설정해 둔다 (요청 시작 지점부터 추적 가능)
        String initialTraceId = UUID.randomUUID().toString();
        MDC.put("traceId", initialTraceId);

        // 2) 검증 통과 후, 도메인 키가 추적에 더 유리하다면 traceId를 교체
        if (request.orderId() != null && request.orderId() > 0) {
            MDC.put("traceId", String.valueOf(request.orderId()));
        }

        try {
            // 로그에는 메시지 본문만 남기고, 메타데이터(traceId)는 패턴에서 자동 출력
            log.info("[OrderController][sendOrderMasterMessage] orderId={}, methodType={}",
                    request.orderId(), request.methodType());

            facade.sendOrderMessage(request);

            return ApiResponse.accepted(new LocalOrderResponse(request.orderId(), HttpStatus.ACCEPTED.name()));
        } finally {
            // 4) ThreadLocal 기반의 MDC는 반드시 정리
            MDC.clear();
        }
    }
}
