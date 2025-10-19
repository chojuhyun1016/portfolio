package org.example.order.api.master.controller.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
import org.example.order.api.master.dto.order.LocalOrderPublishResponse;
import org.example.order.api.master.dto.order.LocalOrderQueryRequest;
import org.example.order.api.master.dto.order.LocalOrderQueryResponse;
import org.example.order.api.master.facade.order.LocalOrderFacade;
import org.example.order.api.master.mapper.order.LocalOrderRequestMapper;
import org.example.order.api.master.mapper.order.LocalOrderResponseMapper;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.support.logging.Correlate;
import org.example.order.common.web.response.ApiResponse;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * 목적
 * - 주문 메시지 발행/전송(API) + 주문 단건 조회(API: 내부에서 가공 덮어쓰기)
 * <p>
 * MDC/trace 전략 (권장, 실무 스타일)
 * - POST: 바디(#p0 또는 #p0.orderId) -> 쿼리스트링(orderId) -> 헤더(X-Order-Id/X-Request-Id/x-request-id)
 * - 컨트롤러에서 @Correlate로 도메인 키(orderId)를 뽑아 traceId로 덮어씀
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/local-orders")
public class LocalOrderController {

    private final LocalOrderFacade facade;
    private final LocalOrderRequestMapper requestMapper;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final LocalOrderResponseMapper responseMapper;

    /**
     * 주문 메시지를 수신하여 내부로 전달한다. (Kafka 전송)
     */
    @PostMapping("/publish")
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
    public ResponseEntity<ApiResponse<LocalOrderPublishResponse>> sendOrderMasterMessage(
            @RequestBody @Valid LocalOrderPublishRequest request,
            HttpServletRequest httpReq
    ) {
        log.info("[LocalOrderController][sendOrderMasterMessage] orderId={}, operation={}",
                request.orderId(), request.operation());

        facade.sendOrderMessage(request);

        return ApiResponse.accepted(new LocalOrderPublishResponse(request.orderId(), HttpStatus.ACCEPTED.name()));
    }

    /**
     * 주문 단건 조회(가공 포함)
     * - Body: JSON { "orderId": 12345 }
     * - 동작: DB 조회 -> 특정 필드 랜덤 델타 -> 가공값으로 덮어쓴 후 반환
     * - 반환: LocalOrderQueryResponse (LocalOrderView -> Response 매핑)
     */
    @PostMapping(
            value = "/query",
            consumes = APPLICATION_JSON_VALUE
    )
    @Correlate(
            paths = {
                    "#p0.orderId",
                    "#p1?.getParameter('orderId')",
                    "#p1?.getHeader('X-Order-Id')",
                    "#p1?.getHeader('X-Request-Id')",
                    "#p1?.getHeader('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public ResponseEntity<ApiResponse<LocalOrderQueryResponse>> findById(
            @RequestBody @Valid LocalOrderQueryRequest req,
            HttpServletRequest httpReq
    ) {
        if (req.getOrderId() == null) {
            log.warn("[LocalOrderController][findById] missing orderId in request body");

            return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
        }

        log.info("[LocalOrderController][findById] orderId={}", req.getOrderId());

        LocalOrderQuery query = requestMapper.toQuery(req);

        var view = facade.findById(query);
        var response = responseMapper.toResponse(view);

        return ApiResponse.ok(response);
    }
}
