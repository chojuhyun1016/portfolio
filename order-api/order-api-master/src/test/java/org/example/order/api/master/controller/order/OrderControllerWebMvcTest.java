package org.example.order.api.master.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.api.master.web.advice.MasterApiExceptionHandler;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MasterApiExceptionHandler.class)
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @MockBean
    private OrderFacade facade;

    @Test
    @DisplayName("POST /order: 정상 요청시 202 ACCEPTED 반환")
    void post_order_should_return_accepted() throws Exception {
        LocalOrderRequest req = new LocalOrderRequest(1L, MessageMethodType.POST);
        Mockito.doNothing().when(facade).sendOrderMessage(any());

        mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @Tag("restdocs")
    @DisplayName("REST Docs 스니펫 생성용: 202 응답")
    void restdocs_snippet() throws Exception {
        LocalOrderRequest req = new LocalOrderRequest(2L, MessageMethodType.PUT);
        Mockito.doNothing().when(facade).sendOrderMessage(any());

        mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isAccepted());
    }
}
