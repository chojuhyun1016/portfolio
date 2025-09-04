package org.example.order.api.master.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.api.master.OrderApiMasterApplication;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실제 컨텍스트 부팅 + MockMvc HTTP 호출
 * - Redis/Redisson/Security/Actuator-Management 보안 자동설정 제외
 * - Kafka 관련 빈은 Mock 으로 대체
 */
@SpringBootTest(
        classes = OrderApiMasterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.main.web-application-type=servlet",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.redisson.spring.starter.RedissonAutoConfigurationV2," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
})
class OrderControllerHttpIT {

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @MockBean
    private KafkaProducerCluster kafkaProducerCluster;

    @Test
    @DisplayName("HTTP 통합: /order 202 응답")
    void post_order_should_return_202(@Autowired MockMvc mvc, @Autowired ObjectMapper om) throws Exception {
        doNothing().when(kafkaProducerService).sendToOrder(any());

        LocalOrderRequest req = new LocalOrderRequest(999L, MessageMethodType.POST);

        mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.orderId").value(999))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }
}
