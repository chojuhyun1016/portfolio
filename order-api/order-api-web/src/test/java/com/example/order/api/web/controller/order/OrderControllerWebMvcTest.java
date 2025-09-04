package com.example.order.api.web.controller.order;

import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import com.example.order.api.web.web.advice.WebApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebApiExceptionHandler.class)
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @MockBean
    private OrderFacade facade;

    @Test
    @DisplayName("GET /order/{id} → 200 OK + data.* 페이로드 검증")
    void findById_should_return_ok_with_payload() throws Exception {
        // given
        OrderResponse resp = new OrderResponse(
                11L,             // id
                101L,               // userId
                "U-0001",           // userNumber
                999L,               // orderId
                "O-20240901-0001",  // orderNumber
                123_456L,           // orderPrice
                false,              // deleteYn
                1L,                 // version
                1725400000000L      // publishedTimestamp (예시)
        );

        Mockito.when(facade.findById(anyLong())).thenReturn(resp);

        // when/then
        mvc.perform(get("/order/{orderId}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.userNumber").value("U-0001"))
                .andExpect(jsonPath("$.data.orderId").value(999))
                .andExpect(jsonPath("$.data.orderNumber").value("O-20240901-0001"))
                .andExpect(jsonPath("$.data.orderPrice").value(123456))
                .andExpect(jsonPath("$.data.deleteYn").value(false))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.publishedTimestamp").value(1725400000000L));
    }
}
