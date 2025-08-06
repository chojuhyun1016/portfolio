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
import org.example.order.core.messaging.order.code.MessageCategory;
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
            String topic = kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ);
            ConsumerFactory<String, String> consumerFactory = (ConsumerFactory<String, String>) kafkaListenerContainerFactory.getConsumerFactory();
            Consumer<String, String> consumer = consumerFactory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX);
            TopicPartition partition = new TopicPartition(topic, 0);
            Set<TopicPartition> partitions = Collections.singleton(partition);

            // 파티션 수동 할당
            consumer.assign(partitions);

            // Topic Partition 조회
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(partitions);

            // 소비할 시점 지정
            if (committedOffsets.get(partition) == null) {
                consumer.seekToBeginning(partitions);
            } else {
                consumer.seek(partition, committedOffsets.get(partition).offset());
            }

            // 마지막 offset - 현재 offset로 읽을 수 있는 전체 메세지 카운트를 구한다.
            long endOffset = consumer.endOffsets(partitions).get(partition);
            long currentOffset = consumer.position(partition);
            long messageCount = endOffset - currentOffset;
            long consumedCount = 0L;

            log.debug("number of messages : {}", messageCount);

            while (consumedCount < messageCount) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(2000));

                // polling 문제가 있을 시
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
