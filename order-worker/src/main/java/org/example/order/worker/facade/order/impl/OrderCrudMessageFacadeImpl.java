package org.example.order.worker.facade.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.order.common.code.MessageMethodType;
import org.example.order.common.exception.CommonException;
import org.example.order.common.utils.jackson.ObjectMapperUtils;
import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.application.order.event.message.OrderCrudEvent;
import org.example.order.core.application.order.event.message.OrderRemoteEvent;
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

        List<OrderCrudEvent> messages = null;
        List<OrderCrudEvent> failureList = new ArrayList<>();

        try {
            messages = records.stream()
                    .map(ConsumerRecord::value)
                    .map(value -> ObjectMapperUtils.valueToObject(value, OrderCrudEvent.class))
                    .toList();

            Map<MessageMethodType, List<OrderCrudEvent>> map = groupingMessages(messages);
            map.forEach((methodType, value) -> {
                try {
                    // Database update
                    orderService.execute(methodType, value);

                    for (OrderCrudEvent message : value) {
                        OrderSyncCommand order = message.getDto().getOrder();

                        if (order.getFailure()) {
                            log.info("failed order : {}", order);
                            failureList.add(message);
                        } else {
                            OrderRemoteEvent orderRemoteEvent = OrderRemoteEvent.toMessage(order.getOrderId(), methodType);
                            kafkaProducerService.sendToOrderRemote(orderRemoteEvent);
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

    private Map<MessageMethodType, List<OrderCrudEvent>> groupingMessages(List<OrderCrudEvent> messages) {
        try {
            return messages.stream()
                    .collect(Collectors.groupingBy(
                            OrderCrudEvent::getMethodType
                    ));
        } catch (Exception e) {
            log.error("error : order crud messages grouping failed : {}", messages);
            log.error(e.getMessage(), e);

            throw new CommonException(WorkerExceptionCode.MESSAGE_GROUPING_FAILED);
        }
    }
}
