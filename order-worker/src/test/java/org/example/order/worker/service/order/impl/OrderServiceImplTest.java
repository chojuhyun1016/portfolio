package org.example.order.worker.service.order.impl;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.List;

import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.message.OrderCrudMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceImplTest {

    private static OrderCrudMessage msgOf(OrderDto dto) {
        OrderCrudMessage msg = mock(OrderCrudMessage.class);
        when(msg.getDto()).thenReturn(dto);

        return msg;
    }

    @Test
    @DisplayName("POST: bulkInsert 호출")
    void execute_post_should_call_bulkInsert() {
        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
        OrderServiceImpl service = new OrderServiceImpl(crud);

        LocalOrderDto order = new LocalOrderDto();
        OrderDto dto = OrderDto.fromInternal(order);
        OrderCrudMessage msg = msgOf(dto);

        service.execute(MessageMethodType.POST, List.of(msg));

        verify(crud, times(1)).bulkInsert(anyList());
        verify(crud, never()).bulkUpdate(anyList());
        verify(crud, never()).deleteAll(anyList());
    }

    @Test
    @DisplayName("PUT: bulkUpdate 호출")
    void execute_put_should_call_bulkUpdate() {
        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
        OrderServiceImpl service = new OrderServiceImpl(crud);

        LocalOrderDto order = new LocalOrderDto();
        OrderDto dto = OrderDto.fromInternal(order);
        OrderCrudMessage msg = msgOf(dto);

        service.execute(MessageMethodType.PUT, List.of(msg));

        verify(crud, times(1)).bulkUpdate(anyList());
        verify(crud, never()).bulkInsert(anyList());
        verify(crud, never()).deleteAll(anyList());
    }

    @Test
    @DisplayName("DELETE: deleteAll 호출")
    void execute_delete_should_call_deleteAll() {
        OrderCrudServiceImpl crud = mock(OrderCrudServiceImpl.class);
        OrderServiceImpl service = new OrderServiceImpl(crud);

        LocalOrderDto order = new LocalOrderDto();
        OrderDto dto = OrderDto.fromInternal(order);
        OrderCrudMessage msg = msgOf(dto);

        service.execute(MessageMethodType.DELETE, List.of(msg));

        verify(crud, times(1)).deleteAll(anyList());
        verify(crud, never()).bulkInsert(anyList());
        verify(crud, never()).bulkUpdate(anyList());
    }
}
