//package org.example.order.api.master;
//
//import org.example.order.api.master.controller.order.OrderController;
//import org.example.order.api.master.facade.order.impl.OrderFacadeImpl;
//import org.example.order.core.application.order.query.OrderCrudDto;
//import org.example.order.core.application.order.query.OrderCrudEntityDto;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
//import org.springframework.security.test.context.support.WithMockUser;
//
//import java.time.LocalDateTime;
//
//import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentRequest;
//import static org.example.order.api.master.utils.ApiDocumentUtils.getDocumentResponse;
//import static org.mockito.Mockito.*;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.payload.PayloadDocumentation.*;
//import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
//import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(OrderController.class)
//@WithMockUser
//public class OrderControllerTest extends AbstractControllerTest {
//
//    @MockBean
//    private OrderFacadeImpl facade;
//
//    @Test
//    public void fetchById() throws Exception {
//        OrderCrudDto dto = makeDto();
//
//        when(facade.fetchById(any())).thenReturn(dto);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders
//                                .get("/order")
//                                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                                .param("orderId", "1"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andDo(
//                        document("order-fetch",
//                                getDocumentRequest(),
//                                getDocumentResponse(),
//                                queryParameters(
//                                        parameterWithName("orderId").description("주문 식별자")
//                                ),
//                                relaxedResponseFields(
//                                        beneathPath("data").withSubsectionId("data"),
//                                        fieldWithPath("order").description("Order 정보")
//                                )
//                        )
//                );
//    }
//
//    private OrderCrudDto makeDto() {
//        OrderCrudDto dto = mock(OrderCrudDto.class);
//        OrderCrudEntityDto order = mock(OrderCrudEntityDto.class);
//
//        when(order.id()).thenReturn(1L);
//        when(order.userId()).thenReturn(1L);
//        when(order.userNumber()).thenReturn("1");
//        when(order.orderId()).thenReturn(1L);
//        when(order.orderNumber()).thenReturn("1");
//        when(order.orderPrice()).thenReturn(1000L);
//        when(order.publishDatetime()).thenReturn(LocalDateTime.now());
//        when(order.deleteYn()).thenReturn(false);
//        when(order.version()).thenReturn(1L);
//
//        when(dto.order()).thenReturn(order);
//
//        return dto;
//    }
//}
