package com.example.order.api.web.controller.order;

import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.logging.Correlate;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderFacade facade;

    @GetMapping("/{orderId}")
    @Correlate(key = "#orderId", mdcKey = "orderId", overrideTraceId = true)
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable Long orderId) {
        log.info("[OrderController][findById] orderId={}", orderId);

        return ApiResponse.ok(facade.findById(orderId));
    }
}
