//package org.example.order.worker.service.order.impl;
//
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.example.order.core.application.order.dto.internal.LocalOrderDto;
//import org.example.order.core.application.order.dto.internal.OrderDto;
//import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.*;
//
///**
// * 서비스 분기 검증: 메서드 타입별 CRUD 위임
// */
//class OrderServiceImplUnitTest {
//
//    private static OrderCrudMessage msg() {
//        LocalOrderDto local = new LocalOrderDto();
//        OrderDto dto = OrderDto.fromInternal(local);
//        OrderCrudMessage m = mock(OrderCrudMessage.class);
//        when(m.getDto()).thenReturn(dto);
//        return m;
//    }
//
//    @Test
//    @DisplayName("POST → bulkInsert")
//    void post_calls_bulkInsert() {
//        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
//        OrderServiceImpl sut = new OrderServiceImpl(crud);
//
//        sut.execute(MessageMethodType.POST, List.of(msg()));
//        verify(crud, times(1)).bulkInsert(anyList());
//        verify(crud, never()).bulkUpdate(anyList());
//        verify(crud, never()).deleteAll(anyList());
//    }
//
//    @Test
//    @DisplayName("PUT → bulkUpdate")
//    void put_calls_bulkUpdate() {
//        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
//        OrderServiceImpl sut = new OrderServiceImpl(crud);
//
//        sut.execute(MessageMethodType.PUT, List.of(msg()));
//        verify(crud, times(1)).bulkUpdate(anyList());
//        verify(crud, never()).bulkInsert(anyList());
//        verify(crud, never()).deleteAll(anyList());
//    }
//
//    @Test
//    @DisplayName("DELETE → deleteAll")
//    void delete_calls_deleteAll() {
//        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
//        OrderServiceImpl sut = new OrderServiceImpl(crud);
//
//        sut.execute(MessageMethodType.DELETE, List.of(msg()));
//        verify(crud, times(1)).deleteAll(anyList());
//        verify(crud, never()).bulkInsert(anyList());
//        verify(crud, never()).bulkUpdate(anyList());
//    }
//}
