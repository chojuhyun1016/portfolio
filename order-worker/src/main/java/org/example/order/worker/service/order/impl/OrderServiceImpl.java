package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.application.order.dto.model.OrderDataModelDto;
import org.example.order.core.messaging.order.message.OrderCrudEvent;
import org.example.order.worker.service.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderCrudServiceImpl orderCrudService;

    // 상위 트랜잭션 실패에 상관없이 정상 일 때 커밋
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void execute(MessageMethodType methodType, List<OrderCrudEvent> messages) {
        List<OrderDataModelDto> dtoList = messages.stream().map(OrderCrudEvent::getDto).toList();

        log.info("{}", dtoList);

        try {
            switch (methodType) {
                case POST -> {
                    orderCrudService.bulkInsert(dtoList.stream().map(OrderDataModelDto::getOrder).toList());
                }
                case PUT -> {
                    orderCrudService.bulkUpdate(dtoList.stream().map(OrderDataModelDto::getOrder).toList());
                }
                case DELETE -> {
                    orderCrudService.deleteAll(messages.stream().map(msg -> msg.getDto().getOrder()).toList());
                }
            }
        } catch (Exception e) {
            log.error("error : execute order failed", e);
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
