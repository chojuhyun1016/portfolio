package com.example.order.api.web;

import com.example.order.api.web.controller.order.OrderController;
import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static com.example.order.api.web.utils.ApiDocumentUtils.getDocumentRequest;
import static com.example.order.api.web.utils.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController 슬라이스 테스트 + REST Docs
 * - GET /order/{orderId}
 * - Facade는 MockBean
 * - WithMockUser: 스프링 시큐리티 통과
 * - 응답은 ApiResponse<data=OrderResponse> 구조
 */
@WebMvcTest(controllers = OrderController.class)
@Import(OrderController.class)
@WithMockUser
class OrderControllerTest extends AbstractControllerTest {

    @MockBean
    private OrderFacade facade;

    @DisplayName("GET /order/{orderId} - 주문 단건 조회 문서화")
    @Test
    void findById_ok() throws Exception {
        // given
        var resp = new OrderResponse(
                1L, 10L, "U-10", 100L, "O-100", 15000L, false, 3L, 1710000000000L
        );
        when(facade.findById(anyLong())).thenReturn(resp);

        // when & then
        mockMvc.perform(
                        get("/order/{orderId}", 100L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "order-find-by-id",
                                getDocumentRequest(),
                                getDocumentResponse(),
                                pathParameters(
                                        parameterWithName("orderId").description("주문 식별자")
                                ),
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
                                        fieldWithPath("publishedTimestamp").description("발행 시각 epoch millis")
                                )
                        )
                );
    }
}
