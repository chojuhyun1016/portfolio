//package org.example.order.api.master.http;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.example.order.api.master.OrderApiMasterApplication;
//import org.example.order.api.master.config.OrderApiMasterConfig;
//import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
//import org.example.order.api.master.facade.order.OrderFacade;
//import org.example.order.api.master.service.common.KafkaProducerService;
//import org.example.order.client.kafka.service.KafkaProducerCluster;
//import org.example.order.common.core.messaging.code.MessageMethodType;
//import org.example.order.contract.shared.op.Operation;
//import org.example.order.core.application.order.mapper.OrderMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.doNothing;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//import org.example.order.common.autoconfigure.web.WebAutoConfiguration;
//import org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration;
//
///**
// * 실제 컨텍스트 부팅 + MockMvc HTTP 호출
// * ------------------------------------------------------------------------
// * 특징
// * - Redis/Redisson/Security/Actuator-Management 보안 자동설정 제외
// * - 외부 인프라 의존성은 Mock 으로 대체하여 HTTP 흐름만 검증
// * - kafka.producer.enabled=false, kafka.consumer.enabled=false : 테스트 시 Kafka 모듈 비활성화
// * <p>
// * (변경 사항)
// * - addFilters = true : CorrelationIdFilter 가 실제 동작하도록 함
// * - @Import(WebAutoConfiguration, LoggingAutoConfiguration) : 오토컨피그를 테스트 컨텍스트에 명시 등록
// * - 컨트롤러(@Correlate) → 파사드 호출 직전 MDC(traceId/orderId/requestId) 값을 캡처/검증
// */
//@SpringBootTest(
//        classes = OrderApiMasterApplication.class,
//        webEnvironment = SpringBootTest.WebEnvironment.MOCK
//)
//@Import({
//        OrderApiMasterConfig.class,
//        WebAutoConfiguration.class,
//        LoggingAutoConfiguration.class
//})
//@AutoConfigureMockMvc(addFilters = true)
//@TestPropertySource(properties = {
//        "spring.main.web-application-type=servlet",
//        "spring.autoconfigure.exclude=" +
//                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
//                "org.redisson.spring.starter.RedissonAutoConfigurationV2," +
//                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
//                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
//                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
//        "kafka.producer.enabled=false",
//        "kafka.consumer.enabled=false"
//})
//class OrderControllerHttpIT {
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
//    private OrderMapper orderMapper;
//
//    @Test
//    @DisplayName("HTTP 통합: /order 202 응답 + X-Request-Id 헤더 존재")
//    void post_order_should_return_202(@Autowired MockMvc mvc, @Autowired ObjectMapper om) throws Exception {
//        doNothing().when(facade).sendOrderMessage(any());
//
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(999L, Operation.CREATE);
//
//        mvc.perform(post("/order")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(req)))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andExpect(header().exists("X-Request-Id"))
//                .andExpect(jsonPath("$.data.orderId").value(999))
//                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
//    }
//
//    @Test
//    @DisplayName("MDC 전파: 컨트롤러(@Correlate) → 파사드 호출 직전 traceId/orderId/requestId 확인")
//    void post_order_should_propagate_mdc_trace_and_orderId(@Autowired MockMvc mvc, @Autowired ObjectMapper om) throws Exception {
//        // 파사드 호출 시점의 MDC를 캡처해 검증
//        doAnswer(invocation -> {
//            String traceId = MDC.get("traceId");
//            String orderId = MDC.get("orderId");
//            String requestId = MDC.get("requestId");
//
//            assertThat(traceId).isEqualTo("1001");
//            assertThat(orderId).isEqualTo("1001");
//            assertThat(requestId).isNotBlank();
//
//            return null;
//        }).when(facade).sendOrderMessage(any());
//
//        LocalOrderPublishRequest req = new LocalOrderPublishRequest(1001L, MessageMethodType.POST);
//
//        mvc.perform(post("/order")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(req)))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andExpect(header().exists("X-Request-Id"));
//    }
//}
