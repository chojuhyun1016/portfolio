//package org.example.order.client.web;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.NoSuchBeanDefinitionException;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * WebConfigDisabledTest
// * <p>
// * - web-client.enabled=false 일 때 WebClient 빈이 생성되지 않아야 함
// * - ApplicationContext 조회 시 NoSuchBeanDefinitionException 발생을 검증
// */
//@SpringBootTest
//@TestPropertySource(properties = {
//        "web-client.enabled=false"
//})
//class WebConfigDisabledTest {
//
//    @org.springframework.beans.factory.annotation.Autowired
//    org.springframework.context.ApplicationContext ctx;
//
//    @Test
//    @DisplayName("web.enabled=false → WebClient 빈 없음")
//    void webClientBeanAbsent() {
//        assertThrows(
//                NoSuchBeanDefinitionException.class,
//                () -> ctx.getBean(org.springframework.web.reactive.function.client.WebClient.class)
//        );
//    }
//}
