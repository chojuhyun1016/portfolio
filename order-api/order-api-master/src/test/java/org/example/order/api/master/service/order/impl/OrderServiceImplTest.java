//package org.example.order.api.master.service.order.impl;
//
//import org.example.order.api.master.service.common.KafkaProducerService;
//import org.example.order.common.core.messaging.code.Operation;
//import org.example.order.core.application.order.dto.command.LocalOrderCommand;
//import org.example.order.core.application.order.mapper.OrderMapper;
//import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class OrderServiceImplTest {
//
//    @Test
//    @DisplayName("Service: command → message 매핑/검증 후 Kafka 전송 호출")
//    void sendMessage_should_map_validate_and_send() {
//        // given
//        KafkaProducerService producer = mock(KafkaProducerService.class);
//        OrderMapper mapper = mock(OrderMapper.class);
//        OrderLocalMessage message = mock(OrderLocalMessage.class);
//
//        when(mapper.toOrderLocalMessage(any())).thenReturn(message);
//
//        LocalOrderServiceImpl service = new LocalOrderServiceImpl(producer, mapper);
//        LocalOrderCommand cmd = new LocalOrderCommand(77L, Operation.PUT);
//
//        // when
//        service.sendMessage(cmd);
//
//        // then
//        verify(mapper, times(1)).toOrderLocalMessage(cmd);
//        verify(message, times(1)).validation();
//        verify(producer, times(1)).sendToOrder(message);
//
//        verifyNoMoreInteractions(producer, mapper);
//    }
//}
