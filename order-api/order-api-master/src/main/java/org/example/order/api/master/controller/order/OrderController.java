package org.example.order.api.master.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.LocalOrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.support.logging.Correlate; // (추가) @Correlate 사용
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 목적
 * - 주문 메시지를 수신하여 내부로 전달한다.
 * <p>
 * MDC/trace 전략 (권장)
 * 1) 요청 초입에서는 CorrelationIdFilter가 requestId를 생성/브리지(MDC["traceId"]=requestId)한다.
 * 2) 컨트롤러 메서드에서 @Correlate로 도메인 키(orderId)를 추출해 traceId를 덮어쓴다.
 * - @Correlate는 실행 전 MDC 백업 → 주입 → 실행 후 복원하므로, 메서드 실행 중(파사드/서비스/카프카) 전 구간에 traceId=orderId가 유지된다.
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
     * @Correlate: 컨트롤러 진입 시점부터 orderId를 traceId로 사용하고, 보조 MDC 키 "orderId"도 함께 저장.
     * - key: 메서드 파라미터명(LocalOrderRequest request)의 필드 접근 → "#request.orderId"
     * - overrideTraceId=true: traceId를 도메인 키로 덮어씀
     * - mdcKey="orderId": 보조키 저장
     */
    @PostMapping
    @Correlate(key = "#request.orderId", mdcKey = "orderId", overrideTraceId = true)
    public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
            @RequestBody @Valid LocalOrderRequest request
    ) {
        log.info("[OrderController][sendOrderMasterMessage] orderId={}, methodType={}",
                request.orderId(), request.methodType());

        facade.sendOrderMessage(request);

        return ApiResponse.accepted(new LocalOrderResponse(request.orderId(), HttpStatus.ACCEPTED.name()));
    }
}
