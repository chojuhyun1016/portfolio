package org.example.order.api.master.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.dto.order.LocalOrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
            @RequestBody @Valid LocalOrderRequest request
    ) {
        log.info("[OrderController][sendOrderMasterMessage] orderId={}, methodType={}",
                request.orderId(), request.methodType());
        facade.sendOrderMessage(request);
        return ApiResponse.accepted(new LocalOrderResponse(request.orderId(), HttpStatus.ACCEPTED.name()));
    }
}
