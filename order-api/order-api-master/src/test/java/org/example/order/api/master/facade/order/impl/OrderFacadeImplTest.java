//package org.example.order.api.master.facade.order.impl;
//
//import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
//import org.example.order.api.master.mapper.order.OrderRequestMapper;
//import org.example.order.api.master.service.order.OrderService;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class OrderFacadeImplTest {
//
//    @Test
//    @DisplayName("Facade: request → mapper → service.sendMessage 호출")
//    void sendOrderMessage_flow() {
//        OrderService service = mock(OrderService.class);
//        OrderRequestMapper mapper = spy(new OrderRequestMapper());
//        OrderFacadeImpl facade = new OrderFacadeImpl(service, mapper);
//
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(10L, MessageMethodType.PUT);
//
//        facade.sendOrderMessage(req);
//
//        verify(mapper, times(1)).toCommand(req);
//        verify(service, times(1)).sendMessage(any());
//        verifyNoMoreInteractions(service);
//    }
//}
