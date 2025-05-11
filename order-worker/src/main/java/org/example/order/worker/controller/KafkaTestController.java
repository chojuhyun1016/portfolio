package org.example.order.worker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.code.type.MessageCategory;
import org.example.order.common.utils.datetime.DateTimeUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class KafkaTestController {

    private final KafkaProducerCluster cluster;
    private final KafkaTopicProperties kafkaTopicProperties;

    /**
     * 예외 발생 test
     */
    @GetMapping("/kafka/retry")
    public String retry() {
        HashMap<String, Object> message = new HashMap<>();
        message.put("id", 1L);
        message.put("publishedTimestamp", DateTimeUtils.localDateTimeToLong(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)));
        cluster.sendMessage(message, kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL));
        return "ok";
    }
}
