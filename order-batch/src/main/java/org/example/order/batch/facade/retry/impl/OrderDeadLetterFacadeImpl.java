package org.example.order.batch.facade.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.infra.messaging.order.code.MessageCategory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterFacadeImpl implements OrderDeadLetterFacade {

    private final OrderDeadLetterService orderDeadLetterService;
    private final ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;
    private final KafkaTopicProperties kafkaTopicProperties;

    private static final String DEAD_LETTER_GROUP_ID = "order-order-dead-letter";
    private static final String CLIENT_SUFFIX = "dlt-client";

    @Override
    public void retry() {
        try {
            // DLQ 토픽
            String topic = kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ);

            // Consumer 생성
            ConsumerFactory<String, String> consumerFactory =
                    (ConsumerFactory<String, String>) kafkaListenerContainerFactory.getConsumerFactory();
            Consumer<String, String> consumer = consumerFactory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX);

            // 파티션 할당
            TopicPartition partition = new TopicPartition(topic, 0);
            Set<TopicPartition> partitions = Collections.singleton(partition);
            consumer.assign(partitions);

            // 시작 offset 지정
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(partitions);
            if (committedOffsets.get(partition) == null) {
                consumer.seekToBeginning(partitions);
            } else {
                consumer.seek(partition, committedOffsets.get(partition).offset());
            }

            // 전체 메시지 수
            long endOffset = consumer.endOffsets(partitions).get(partition);
            long currentOffset = consumer.position(partition);
            long messageCount = endOffset - currentOffset;
            long consumedCount = 0L;

            log.debug("number of messages : {}", messageCount);

            // 메시지 처리 루프
            while (consumedCount < messageCount) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(2000));

                if (records.count() == 0) {
                    throw new CommonException(BatchExceptionCode.POLLING_FAILED);
                }

                for (ConsumerRecord<String, String> record : records.records(topic)) {
                    log.info("{}", record);
                    orderDeadLetterService.retry(record.value());
                }

                consumedCount += records.count();
                consumer.commitSync();
            }

            consumer.close();
        } catch (Exception e) {
            log.error("error : order dead letter retry failed", e);

            throw e;
        }
    }
}
