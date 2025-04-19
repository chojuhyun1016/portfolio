package org.example.order.api.master.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.web.ApiResponse;
import org.example.order.core.application.dto.OrderCrudDto;
import org.example.order.core.application.dto.OrderRemoteMessageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/order")
public class OrderController {
    private final OrderFacade facade;

    @GetMapping
    public ResponseEntity<ApiResponse<OrderCrudDto>> fetchById(@RequestParam Long orderId) {
        return ApiResponse.ok(facade.fetchById(orderId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendOrderMasterMessage(@RequestBody @Valid OrderRemoteMessageDto message) {
        facade.sendOrderMessage(message);
        return ApiResponse.ok(null);
    }
}
