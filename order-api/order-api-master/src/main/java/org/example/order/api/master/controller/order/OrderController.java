package org.example.order.api.master.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.response.CommonResponse;
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
    public ResponseEntity<CommonResponse<OrderCrudDto>> fetchById(@RequestParam Long orderId) {
        return CommonResponse.ok(facade.fetchById(orderId));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Void>> sendOrderMasterMessage(@RequestBody @Valid OrderRemoteMessageDto message) {
        facade.sendOrderMessage(message);
        return CommonResponse.ok(null);
    }
}
