package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.code.MessageMethodType;
import org.example.order.core.application.dto.order.OrderDto;
import org.example.order.core.application.message.order.OrderCrudMessage;
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
    public void execute(MessageMethodType methodType, List<OrderCrudMessage> messages) {
        List<OrderDto> dtoList = messages.stream().map(OrderCrudMessage::getDto).toList();

        log.info("{}", dtoList);

        try {
            switch (methodType) {
                case POST -> {
                    orderCrudService.bulkInsert(dtoList.stream().map(OrderDto::getOrder).toList());
                }
                case PUT -> {
                    orderCrudService.bulkUpdate(dtoList.stream().map(OrderDto::getOrder).toList());
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
