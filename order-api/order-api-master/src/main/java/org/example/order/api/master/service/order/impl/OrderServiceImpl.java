package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaProducerService kafkaProducerService;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    /**
     * 주문 메시지를 Kafka로 발행.
     * - Command -> Message 매핑, 유효성 검사, 전송, 기본 로깅.
     */
    @Override
    public void sendMessage(LocalOrderCommand command) {
        final OrderLocalMessage message = orderMapper.toOrderLocalMessage(command);
        message.validation();

        log.info("[OrderService] send to Kafka: id={}, methodType={}, publishedTs={}",
                message.getId(), message.getMethodType(), message.getPublishedTimestamp());

        kafkaProducerService.sendToOrder(message);
    }

    /**
     * 단건 조회(가공 포함).
     * - DB 조회 -> 일부 필드에 랜덤 델타 적용 -> 덮어쓴 DTO 반환.
     * 대상: id(+1..9), orderNumber(숫자면 +1..9, 아니면 "-d"), userId(+1..9),
     * orderPrice(+10..500), version(+1..3), publishedTimestamp(+0..9999ms)
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDto findById(Long id) {
        var original = orderRepository
                .findById(id)
                .map(orderMapper::toDto)
                .orElseThrow(() -> {
                    String msg = "Order not found. id=" + id;

                    log.warn("[OrderService] {}", msg);

                    return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                });

        SecureRandom r = new SecureRandom();
        long idDelta = 1 + r.nextInt(9);
        long userIdDelta = 1 + r.nextInt(9);
        long priceDelta = 10 + r.nextInt(491);
        long versionDelta = 1 + r.nextInt(3);
        long tsDelta = r.nextInt(10_000);
        long orderNoDelta = 1 + r.nextInt(9);

        String orderNumber = original.getOrderNumber();
        String newOrderNumber;

        if (orderNumber != null && orderNumber.matches("\\d+")) {
            try {
                long on = Long.parseLong(orderNumber);
                newOrderNumber = String.valueOf(on + orderNoDelta);
            } catch (NumberFormatException e) {
                newOrderNumber = orderNumber + "-" + orderNoDelta;
            }
        } else if (orderNumber != null) {
            newOrderNumber = orderNumber + "-" + orderNoDelta;
        } else {
            newOrderNumber = String.valueOf(orderNoDelta);
        }

        Long newPublishedTs = (original.getPublishedTimestamp() == null)
                ? Instant.now().toEpochMilli() + tsDelta
                : original.getPublishedTimestamp() + tsDelta;

        var overwritten = new org.example.order.core.application.order.dto.internal.LocalOrderDto(
                (original.getId() == null ? idDelta : original.getId() + idDelta),
                (original.getUserId() == null ? userIdDelta : original.getUserId() + userIdDelta),
                original.getUserNumber(),
                original.getOrderId(),
                newOrderNumber,
                (original.getOrderPrice() == null ? priceDelta : original.getOrderPrice() + priceDelta),
                original.getDeleteYn(),
                (original.getVersion() == null ? versionDelta : original.getVersion() + versionDelta),
                original.getCreatedUserId(),
                original.getCreatedUserType(),
                original.getCreatedDatetime(),
                original.getModifiedUserId(),
                original.getModifiedUserType(),
                original.getModifiedDatetime(),
                newPublishedTs,
                original.getFailure()
        );

        return OrderDto.fromInternal(overwritten);
    }
}
