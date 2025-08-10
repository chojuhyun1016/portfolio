package org.example.order.api.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.api.master.controller.order.OrderController;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;

import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentRequest;
import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController 슬라이스 테스트 + REST Docs
 * - 컨트롤러는 POST /order 제공
 * - Facade 시그니처는 sendOrderMessage(LocalOrderRequest)
 * - 응답은 ApiResponse<LocalOrderResponse> 래핑 구조
 * - CSRF 적용 환경이므로 요청에 .with(csrf()) 추가
 * - enum 값은 하드코딩하지 않고 런타임에서 안전하게 선택
 */
@WebMvcTest(controllers = OrderController.class)
@WithMockUser
class OrderControllerTest extends AbstractControllerTest {

    @MockBean
    private OrderFacade facade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("POST /order - 로컬 주문 메시지 전송, 202 Accepted와 응답 바디 문서화")
    @Test
    void sendOrderMessage_accepted() throws Exception {
        // given
        doNothing().when(facade).sendOrderMessage(any(LocalOrderRequest.class));

        // enum을 하드코딩하지 않고, 실제 프로젝트에 정의된 첫 번째 상수를 사용
        MessageMethodType anyValidMethod = MessageMethodType.values()[0];

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", 1L);
        payload.put("methodType", anyValidMethod.name()); // Jackson 기본 전략: name() 문자열과 매칭

        String json = objectMapper.writeValueAsString(payload);

        // when & then
        mockMvc.perform(
                        post("/order")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andDo(print())
                .andExpect(status().isAccepted())
                .andDo(
                        document(
                                "order-send",
                                getDocumentRequest(),
                                getDocumentResponse(),
                                requestFields(
                                        fieldWithPath("orderId").description("주문 ID"),
                                        fieldWithPath("methodType").description("메서드 타입 enum 문자열")
                                ),
                                // 응답 data 섹션만 느슨하게 문서화 (LocalOrderResponse { orderId, status })
                                relaxedResponseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("orderId").description("요청된 주문 ID"),
                                        fieldWithPath("status").description("처리 상태 문자열")
                                )
                        )
                );
    }
}
