//package org.example.order.client.kafka;
//
//import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.NoSuchBeanDefinitionException;
//import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * producer.enabled=false 일 때 Producer 관련 빈이 생성되지 않아야 한다.
// * - KafkaTemplate 미생성
// * - KafkaProducerCluster 미생성
// */
//@SpringBootTest // [CHANGED] classes 지정 제거
//@ImportAutoConfiguration(KafkaAutoConfiguration.class)
//@TestPropertySource(properties = {
//        "kafka.producer.enabled=false",
//        "kafka.ssl.enabled=false"
//})
//class KafkaProducerConfigDisabledTest {
//
//    @org.springframework.beans.factory.annotation.Autowired
//    private org.springframework.context.ApplicationContext ctx;
//
//    @Test
//    @DisplayName("producer.enabled=false → KafkaTemplate/KafkaProducerCluster 미생성")
//    void kafkaBeansAbsentWhenDisabled() {
//        assertThrows(
//                NoSuchBeanDefinitionException.class,
//                () -> ctx.getBean(org.springframework.kafka.core.KafkaTemplate.class),
//                "KafkaTemplate must NOT be created when enabled=false"
//        );
//
//        assertThrows(
//                NoSuchBeanDefinitionException.class,
//                () -> ctx.getBean(org.example.order.client.kafka.service.KafkaProducerCluster.class),
//                "KafkaProducerCluster must NOT be created when enabled=false"
//        );
//    }
//}
