package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.code.MessageMethodType;
import org.example.order.common.exception.CommonException;
import org.example.order.common.utils.ObjectMapperUtils;
import org.example.order.core.application.dto.OrderLocalDto;
import org.example.order.core.application.message.OrderCrudMessage;
import org.example.order.core.application.message.OrderRemoteMessage;
import org.example.order.worker.exception.DatabaseExecuteException;
import org.example.order.worker.exception.WorkerExceptionCode;
import org.example.order.worker.facade.order.OrderCrudMessageFacade;
import org.example.order.worker.service.common.KafkaProducerService;
import org.example.order.worker.service.order.OrderService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCrudMessageFacadeImpl implements OrderCrudMessageFacade {
    private final KafkaProducerService kafkaProducerService;
    private final OrderService orderService;

    @Transactional
    @Override
    public void executeOrderCrud(List<ConsumerRecord<String, Object>> records) {
        if (ObjectUtils.isEmpty(records)) {
            return;
        }

        log.debug(" order master crud records : {}", records);

        List<OrderCrudMessage> messages = null;
        List<OrderCrudMessage> failureList = new ArrayList<>();

        try {
            messages = records.stream()
                    .map(ConsumerRecord::value)
                    .map(value -> ObjectMapperUtils.valueToObject(value, OrderCrudMessage.class))
                    .toList();

            Map<MessageMethodType, List<OrderCrudMessage>> map = groupingMessages(messages);
            map.forEach((methodType, value) -> {
                try {
                    // Database update
                    orderService.execute(methodType, value);

                    for (OrderCrudMessage message : value) {
                        OrderLocalDto order = message.getDto().getOrder();

                        if (order.getFailure()) {
                            log.info("failed order : {}", order);
                            failureList.add(message);
                        } else {
                            OrderRemoteMessage orderRemoteMessage = OrderRemoteMessage.toMessage(order.getOrderId(), methodType);
                            kafkaProducerService.sendToOrderRemote(orderRemoteMessage);
                        }
                    }
                } catch (Exception e) {
                    log.error("error : order crud message : {}", value, e);
                    kafkaProducerService.sendToDlq(value, e);
                }
            });

            if (!failureList.isEmpty()) {
                throw new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED);
            }
        } catch (DatabaseExecuteException e) {
            kafkaProducerService.sendToDlq(failureList, e);
            throw e;
        } catch (Exception e) {
            log.error("error : order crud messages", e);
            kafkaProducerService.sendToDlq(messages, e);
            throw e;
        }
    }

    private Map<MessageMethodType, List<OrderCrudMessage>> groupingMessages(List<OrderCrudMessage> messages) {
        try {
            return messages.stream()
                    .collect(Collectors.groupingBy(
                            OrderCrudMessage::getMethodType
                    ));
        } catch (Exception e) {
            log.error("error : order crud messages grouping failed : {}", messages);
            log.error(e.getMessage(), e);

            throw new CommonException(WorkerExceptionCode.MESSAGE_GROUPING_FAILED);
        }
    }
}
