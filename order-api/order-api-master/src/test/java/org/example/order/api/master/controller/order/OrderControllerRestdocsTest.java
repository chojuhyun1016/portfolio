//package org.example.order.api.master.controller.order;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.example.order.api.master.dto.order.LocalOrderRequest;
//import org.example.order.api.master.facade.order.OrderFacade;
//import org.example.order.api.master.service.common.KafkaProducerService;
//import org.example.order.api.master.web.advice.MasterApiExceptionHandler;
//import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
//import org.example.order.client.kafka.service.KafkaProducerCluster;
//import org.example.order.common.core.messaging.code.Operation;
//import org.example.order.core.application.order.mapper.OrderMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.context.annotation.ComponentScan.Filter;
//import org.springframework.context.annotation.FilterType;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
//import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
//import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
//import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
//import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * REST Docs 슬라이스 테스트
// * - Controller 슬라이스만 로드
// * - Facade/Service/KafkaCluster/TopicProperties 는 MockBean 으로 대체
// * - 서비스/파사드 "구현체"는 슬라이스에서 명시적으로 제외
// * - 혹시 서비스가 들어오더라도 OrderMapper 를 Mock 으로 제공해 의존성 충족
// */
//@WebMvcTest(
//        controllers = OrderController.class,
//        excludeFilters = {
//                @Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\..*\\.service\\..*"),
//                @Filter(type = FilterType.REGEX, pattern = "org\\.example\\.order\\..*\\.facade\\..*\\.impl\\..*")
//        }
//)
//@Import(MasterApiExceptionHandler.class)
//@AutoConfigureMockMvc(addFilters = false)
//@AutoConfigureRestDocs
//class OrderControllerRestdocsTest {
//
//    @Autowired
//    private MockMvc mvc;
//
//    @Autowired
//    private ObjectMapper om;
//
//    @MockBean
//    private OrderFacade facade;
//
//    @MockBean
//    private KafkaProducerService kafkaProducerService;
//
//    @MockBean
//    private KafkaProducerCluster kafkaProducerCluster;
//
//    @MockBean
//    private KafkaTopicProperties kafkaTopicProperties;
//
//    @MockBean
//    private OrderMapper orderMapper;
//
//    @Test
//    @Tag("restdocs")
//    @DisplayName("REST Docs: POST /order → 202")
//    void restdocs_order_accepted() throws Exception {
//        Mockito.doNothing().when(facade).sendOrderMessage(any());
//
//        var req = new LocalOrderRequest(999L, Operation.POST);
//
//        mvc.perform(RestDocumentationRequestBuilders.post("/order")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(om.writeValueAsString(req)))
//                .andExpect(status().isAccepted())
//                .andDo(document("order-accepted",
//                        preprocessRequest(prettyPrint()),
//                        preprocessResponse(prettyPrint()),
//                        relaxedResponseFields(
//                                fieldWithPath("metadata").description("응답 메타데이터").optional(),
//                                fieldWithPath("metadata.code").description("응답 코드(예: 200/202 등)").optional(),
//                                fieldWithPath("metadata.msg").description("응답 메시지").optional(),
//                                fieldWithPath("metadata.timestamp").description("응답 생성 시각(ISO)").optional(),
//                                fieldWithPath("data").description("응답 데이터"),
//                                fieldWithPath("data.orderId").description("주문 ID"),
//                                fieldWithPath("data.status").description("상태 (예: ACCEPTED)")
//                        )
//                ));
//    }
//}
