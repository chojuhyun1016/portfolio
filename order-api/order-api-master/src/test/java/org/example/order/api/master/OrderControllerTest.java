package org.example.order.api.master;

import org.example.order.api.master.controller.order.OrderController;
import org.example.order.api.master.dto.order.OrderResponse;
import org.example.order.api.master.facade.order.OrderFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.test.context.support.WithMockUser;

import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentRequest;
import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController REST Docs + 슬라이스 테스트
 * - WebMvcTest로 컨트롤러 계층만 로딩
 * - Facade는 MockBean으로 대체
 * - WithMockUser로 시큐리티 통과
 */
@WebMvcTest(controllers = OrderController.class)
@WithMockUser
class OrderControllerTest extends AbstractControllerTest {

    @MockBean
    private OrderFacade facade; // 컨트롤러가 주입받는 타입과 정확히 동일

    @DisplayName("GET /order?orderId=1 - 주문 단건 조회 문서화")
    @Test
    void fetchById() throws Exception {
        // given: 컨트롤러는 facade.findById(Long)를 호출한다.
        var response = new OrderResponse(
                1L,            // id
                1L,            // userId
                "1",           // userNumber
                1L,            // orderId
                "1",           // orderNumber
                1000L,         // orderPrice
                false,         // deleteYn
                1L,            // version
                1710000000000L // publishedTimestamp (예시)
        );
        when(facade.findById(anyLong())).thenReturn(response);

        // when & then
        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/order")
                                .queryParam("orderId", "1")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "order-fetch",
                                getDocumentRequest(),
                                getDocumentResponse(),
                                // 요청 파라미터 문서화
                                queryParameters(
                                        parameterWithName("orderId").description("주문 식별자")
                                ),
                                // 응답 바디 중 data 섹션만 느슨하게 문서화
                                // OrderResponse 필드 기준으로 기술
                                relaxedResponseFields(
                                        beneathPath("data").withSubsectionId("data"),
                                        fieldWithPath("id").description("ID"),
                                        fieldWithPath("userId").description("사용자 ID"),
                                        fieldWithPath("userNumber").description("사용자 번호"),
                                        fieldWithPath("orderId").description("주문 ID"),
                                        fieldWithPath("orderNumber").description("주문 번호"),
                                        fieldWithPath("orderPrice").description("주문 금액"),
                                        fieldWithPath("deleteYn").description("삭제 여부"),
                                        fieldWithPath("version").description("버전"),
                                        fieldWithPath("publishedTimestamp").description("발행 시각 (epoch millis)")
                                )
                        )
                );
    }
}
