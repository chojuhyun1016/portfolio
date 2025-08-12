package org.example.order.api.master.restdocs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.order.api.master.controller.order.OrderController;
import org.example.order.api.master.dto.order.LocalOrderRequest;
import org.example.order.api.master.facade.order.OrderFacade;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@WithMockUser
class OrderControllerDocsTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    OrderFacade facade;

    private final ObjectMapper om = new ObjectMapper();

    @DisplayName("[DOCS] POST /order - 주문 메시지 문서화(Mock)")
    @Test
    void sendOrder_docs() throws Exception {
        doNothing().when(facade).sendOrderMessage(any(LocalOrderRequest.class));

        var payload = Map.of(
                "orderId", 1L,
                "methodType", MessageMethodType.values()[0].name()
        );

        mockMvc.perform(
                        post("/order")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(payload))
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                        "order-send",
                        preprocessRequest(modifyHeaders().remove("X-Internal-Auth"), prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("methodType").description("메서드 타입 enum")
                        ),
                        relaxedResponseFields(
                                beneathPath("data").withSubsectionId("data"),
                                fieldWithPath("orderId").description("요청된 주문 ID"),
                                fieldWithPath("status").description("처리 상태")
                        )
                ));
    }
}
