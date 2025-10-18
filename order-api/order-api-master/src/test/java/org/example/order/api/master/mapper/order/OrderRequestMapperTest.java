//package org.example.order.api.master.mapper.order;
//
//import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.example.order.core.application.order.dto.command.LocalOrderCommand;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class OrderRequestMapperTest {
//
//    @Test
//    @DisplayName("LocalOrderPublishRequest → LocalOrderCommand 매핑")
//    void toCommand_should_map_correctly() {
//        LocalOrderRequestMapper mapper = new LocalOrderRequestMapper();
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(123L, MessageMethodType.POST);
//
//        LocalOrderCommand cmd = mapper.toCommand(req);
//
//        assertThat(cmd).isNotNull();
//        assertThat(cmd.orderId()).isEqualTo(123L);
//        assertThat(cmd.methodType()).isEqualTo(MessageMethodType.POST);
//    }
//
//    @Test
//    @DisplayName("null 입력 시 null 반환")
//    void toCommand_null_returns_null() {
//        LocalOrderRequestMapper mapper = new LocalOrderRequestMapper();
//        assertThat(mapper.toCommand(null)).isNull();
//    }
//}
