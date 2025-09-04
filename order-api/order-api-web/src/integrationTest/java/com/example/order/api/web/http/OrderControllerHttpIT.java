package com.example.order.api.web.http;

import com.example.order.api.web.controller.order.OrderController;
import com.example.order.api.web.dto.order.OrderResponse;
import com.example.order.api.web.facade.order.OrderFacade;
import com.example.order.api.web.web.advice.WebApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 한 파일만 사용하는 Standalone MockMvc + REST Docs 테스트
 * - 스프링 컨텍스트/Autowired 없이 수행 → 외부 인프라 영향 제거
 * - @Tag("restdocs") : ./gradlew :order-api:order-api-web:rest 로 스니펫 생성
 * - 문서 스니펫: build/generated-snippets/order-get-by-id/*
 */
@ExtendWith(RestDocumentationExtension.class)
class OrderControllerHttpIT {

    @Test
    @Tag("restdocs")
    @DisplayName("REST Docs: GET /order/{id} → 200 OK (Standalone)")
    void get_order_should_return_200(RestDocumentationContextProvider restDocs) throws Exception {
        // given
        OrderFacade facade = Mockito.mock(OrderFacade.class);
        Long id = 101L;
        OrderResponse resp = new OrderResponse(
                id,              // id
                1000L,           // userId
                "U-0001",        // userNumber
                5555L,           // orderId
                "O-5555",        // orderNumber
                99000L,          // orderPrice
                false,           // deleteYn
                1L,              // version
                1720000000000L   // publishedTimestamp
        );

        when(facade.findById(id)).thenReturn(resp);

        OrderController controller = new OrderController(facade);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new WebApiExceptionHandler())
                .apply(documentationConfiguration(restDocs))
                .build();

        // when/then (스키마: metadata + data 만 검증)
        mvc.perform(get("/order/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.code").value(200))
                .andExpect(jsonPath("$.metadata.msg").exists())
                .andExpect(jsonPath("$.metadata.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.orderId").value(5555))
                .andExpect(jsonPath("$.data.orderNumber").value("O-5555"))
                .andDo(document("order-get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }
}
