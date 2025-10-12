//package org.example.order.client.kafka;
//
//import org.example.order.client.kafka.autoconfig.KafkaAutoConfiguration;
//import org.example.order.client.kafka.service.KafkaProducerCluster;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.ApplicationContext;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.test.context.TestPropertySource;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@ImportAutoConfiguration(KafkaAutoConfiguration.class)
//@TestPropertySource(properties = {
//        "kafka.producer.enabled=true",
//        "kafka.producer.bootstrap-servers=localhost:9092",
//        "kafka.ssl.enabled=false"
//})
//class KafkaProducerConfigEnabledTest {
//
//    @Autowired
//    ApplicationContext ctx;
//
//    @Autowired
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//    private KafkaTemplate<String, Object> template;
//
//    @Autowired
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//    private KafkaProducerCluster cluster;
//
//    @Test
//    @DisplayName("producer.enabled=true → KafkaTemplate/KafkaProducerCluster 생성")
//    void kafkaBeansPresentWhenEnabled() {
//        // 런타임 레벨로도 확인 (설정 누락 시 여기서 명확히 실패)
//        assertNotNull(ctx.getBean(KafkaTemplate.class));
//        assertNotNull(ctx.getBean(KafkaProducerCluster.class));
//
//        assertNotNull(template, "KafkaTemplate must be created when enabled=true");
//        assertNotNull(cluster, "KafkaProducerCluster must be created when enabled=true");
//    }
//}
