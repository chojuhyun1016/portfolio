package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.LocalOrderService;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.example.order.core.application.order.dto.view.LocalOrderView;
import org.example.order.core.application.order.mapper.LocalOrderMapper;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.domain.order.repository.LocalOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalOrderServiceImpl implements LocalOrderService {

    private final KafkaProducerService kafkaProducerService;
    private final LocalOrderMapper localOrderMapper;
    private final LocalOrderRepository localOrderRepository;

    /**
     * 주문 메시지를 Kafka로 발행.
     * - Command -> Message 매핑, 유효성 검사, 전송, 기본 로깅.
     */
    @Override
    public void sendMessage(LocalOrderCommand command) {
        final OrderLocalMessage message = localOrderMapper.toOrderLocalMessage(command);

        message.validation();

        log.info("[LocalOrderService] send to Kafka: id={}, operation={}, publishedTs={}",
                message.getId(), message.getOperation(), message.getPublishedTimestamp());

        kafkaProducerService.sendToOrder(message);
    }

    /**
     * 단건 조회(가공 포함).
     * - DB 조회 -> 일부 필드에 랜덤 델타 적용 -> 덮어쓴 값으로 View 반환.
     */
    @Override
    @Transactional(readOnly = true)
    public LocalOrderView findById(LocalOrderQuery query) {
        Long id = query.orderId();

        // Entity -> LocalOrderView (MapStruct)
        LocalOrderView original = localOrderRepository
                .findById(id)
                .map(localOrderMapper::toView)
                .orElseThrow(() -> {
                    String msg = "Order not found. id=" + id;
                    log.warn("[LocalOrderService] {}", msg);

                    return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                });

        // 덮어쓴 View 구성 (불변 DTO이므로 빌더로 새로 생성)
        LocalOrderView overwritten = LocalOrderView.builder()
                .id(original.getId())
                .userId(original.getUserId())
                .userNumber(original.getUserNumber())
                .orderId(original.getOrderId())
                .orderNumber(original.getOrderNumber())
                .orderPrice(original.getOrderPrice())
                .deleteYn(original.getDeleteYn())
                .version(original.getVersion())
                .createdUserId(original.getCreatedUserId())
                .createdUserType(original.getCreatedUserType())
                .createdDatetime(original.getCreatedDatetime())
                .modifiedUserId(original.getModifiedUserId())
                .modifiedUserType(original.getModifiedUserType())
                .modifiedDatetime(original.getModifiedDatetime())
                .publishedTimestamp(original.getPublishedTimestamp())
                .failure(Boolean.TRUE.equals(original.getFailure()))
                .build();

        return overwritten;
    }
}
