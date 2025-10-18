//package org.example.order.api.master.controller.order;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
//import org.example.order.api.master.facade.order.OrderFacade;
//import org.example.order.api.master.service.common.KafkaProducerService;
//import org.example.order.api.master.web.advice.MasterApiExceptionHandler;
//import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
//import org.example.order.client.kafka.service.KafkaProducerCluster;
//import org.example.order.common.core.messaging.code.Operation;
//import org.example.order.core.application.order.mapper.OrderMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.ComponentScan.Filter;
//import org.springframework.context.annotation.FilterType;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * WebMvc 슬라이스 테스트
// * - Controller 슬라이스만 로드
// * - Facade/Service/KafkaCluster/TopicProperties/OrderMapper 는 MockBean 으로 대체
// * - 서비스/파사드 구현체는 명시 제외
// */
//@WebMvcTest(
//        controllers = OrderController.class,
//        excludeFilters = {
//                @Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\..*\\.service\\..*"),
//                @Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\..*\\.facade\\..*\\.impl\\..*")
//        }
//)
//@AutoConfigureMockMvc(addFilters = false)
//@Import(MasterApiExceptionHandler.class)
//class OrderControllerWebMvcTest {
//
//    @Autowired
//    private MockMvc mvc;
//
//    @Autowired
//    private ObjectMapper om;
//
//    @MockBean
//    private OrderFacade facade;
//
//    @MockBean
//    private KafkaProducerService kafkaProducerService;
//
//    @MockBean
//    private KafkaProducerCluster kafkaProducerCluster;
//
//    @MockBean
//    private KafkaTopicProperties kafkaTopicProperties;
//
//    @MockBean
//    private OrderMapper orderMapper;
//
//    @Test
//    @DisplayName("POST /order: 정상 요청시 202 ACCEPTED 반환")
//    void post_order_should_return_accepted() throws Exception {
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(1L, Operation.POST);
//        Mockito.doNothing().when(facade).sendOrderMessage(any());
//
//        mvc.perform(post("/order")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(req)))
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.data.orderId").value(1))
//                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
//    }
//
//    @Test
//    @Tag("restdocs")
//    @DisplayName("REST Docs 스니펫 생성용: 202 응답")
//    void restdocs_snippet() throws Exception {
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(2L, Operation.PUT);
//        Mockito.doNothing().when(facade).sendOrderMessage(any());
//
//        mvc.perform(post("/order")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(req)))
//                .andExpect(status().isAccepted());
//    }
//}
