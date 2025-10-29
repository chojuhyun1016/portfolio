//package com.example.order.api.web.controller.order;
//
//import com.example.order.api.web.dto.order.OrderResponse;
//import com.example.order.api.web.facade.order.OrderFacade;
//import com.example.order.api.web.web.advice.WebApiExceptionHandler;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.*;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
//import static org.springframework.restdocs.payload.PayloadDocumentation.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * REST Docs 스니펫 생성 테스트
// * - relaxedResponseFields 사용: 최상위(success/metadata 등)는 환경별 상이 → 제외
// * - data.* 하위만 문서화하여 스니펫 불일치 예외 방지
// */
//@WebMvcTest(controllers = OrderController.class)
//@AutoConfigureMockMvc(addFilters = false)
//@AutoConfigureRestDocs
//@Import(WebApiExceptionHandler.class)
//class OrderControllerRestdocsTest {
//
//    @Autowired
//    private MockMvc mvc;
//    @Autowired
//    private ObjectMapper om;
//
//    @MockBean
//    private OrderFacade facade;
//
//    @Test
//    @Tag("restdocs")
//    @DisplayName("REST Docs: GET /order/{id} → 200 (data.* 문서화)")
//    void restdocs_get_order() throws Exception {
//        // given
//        OrderResponse resp = new OrderResponse(
//                11L, 101L, "U-0001",
//                999L, "O-20240901-0001",
//                123_456L, false, 1L, 1725400000000L
//        );
//
//        Mockito.when(facade.findById(anyLong())).thenReturn(resp);
//
//        // when/then
//        mvc.perform(RestDocumentationRequestBuilders.get("/order/{orderId}", 999L)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(document("order-get",
//                        preprocessRequest(prettyPrint()),
//                        preprocessResponse(prettyPrint()),
//                        relaxedResponseFields(
//                                fieldWithPath("data.id").description("엔티티 식별자"),
//                                fieldWithPath("data.userId").description("사용자 ID"),
//                                fieldWithPath("data.userNumber").description("사용자 번호"),
//                                fieldWithPath("data.orderId").description("주문 ID"),
//                                fieldWithPath("data.orderNumber").description("주문 번호"),
//                                fieldWithPath("data.orderPrice").description("주문 금액"),
//                                fieldWithPath("data.deleteYn").description("삭제 여부"),
//                                fieldWithPath("data.version").description("버전"),
//                                fieldWithPath("data.publishedTimestamp").description("발행 타임스탬프(에포크 ms)")
//                        )
//                ));
//    }
//}
