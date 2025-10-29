package org.example.order.api.web.service.order.impl;

import org.example.order.api.web.service.order.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.application.order.dto.query.OrderQuery;
import org.example.order.core.application.order.dto.view.OrderView;
// ↓ 동일 패키지 (org.example)
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.example.order.core.application.order.cache.OrderCacheService; // ← ★ core 서비스 사용
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order Query Service 구현
 * - DB별 조회 후 Application View로 투영
 * - 주의: Repository 시그니처/키 정책은 실제 엔티티 스키마에 맞춰 조정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;                 // JPA (MySQL 등)
    private final OrderDynamoRepository orderDynamoRepository;     // DynamoDB (Enhanced Client)
    private final OrderMapper orderMapper;                         // Entity/Sync/View 매퍼

    // ★ Redis 직접 접근 제거 → core 서비스로 위임
    private final OrderCacheService orderCacheService;

    /**
     * MySQL(JPA) 조회
     */
    @Override
    @Transactional(readOnly = true)
    public OrderView findByMySql(OrderQuery query) {
        Long id = query.orderId();

        OrderEntity entity = orderRepository
                .findById(id)
                .orElseThrow(() -> {
                    String msg = "Order not found in MySQL. id=" + id;
                    log.warn("[OrderQueryService][MySQL] {}", msg);

                    return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                });

        return orderMapper.toView(entity);
    }

    /**
     * DynamoDB 조회
     */
    @Override
    @Transactional(readOnly = true)
    public OrderView findByDynamo(OrderQuery query) {
        String id = String.valueOf(query.orderId());

        var opt = orderDynamoRepository.findById(id);

        var item = opt.orElseThrow(() -> {
            String msg = "Order not found in DynamoDB. id=" + id;
            log.warn("[OrderQueryService][Dynamo] {}", msg);

            return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
        });

        Boolean deleteYnBool = toBooleanYN(item.getDeleteYn());

        return OrderView.builder()
                .id(item.getOrderId())
                .userId(item.getUserId())
                .userNumber(item.getUserNumber())
                .orderId(item.getOrderId())
                .orderNumber(item.getOrderNumber())
                .orderPrice(item.getOrderPrice())
                .deleteYn(deleteYnBool)
                .version(null)
                .createdUserId(item.getCreatedUserId())
                .createdUserType(item.getCreatedUserType())
                .createdDatetime(item.getCreatedDatetime())
                .modifiedUserId(item.getModifiedUserId())
                .modifiedUserType(item.getModifiedUserType())
                .modifiedDatetime(item.getModifiedDatetime())
                .publishedTimestamp(item.getPublishedTimestamp())
                .failure(Boolean.FALSE)
                .build();
    }

    /**
     * Redis(캐시) 조회
     * - core의 OrderCacheService를 통해 조회/매핑 일원화
     */
    @Override
    @Transactional(readOnly = true)
    public OrderView findByRedis(OrderQuery query) {
        final Long orderId = query.orderId();

        return orderCacheService.getViewByOrderId(orderId)
                .orElseThrow(() -> {
                    String key = "order:" + orderId; // 로깅용
                    String msg = "Order not found in Redis. key=" + key;
                    log.warn("[OrderQueryService][Redis] {}", msg);
                    return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                });
    }

    /* ------------------------ helpers ------------------------ */
    private static Boolean toBooleanYN(Object any) {
        if (any == null) {
            return null;
        }

        if (any instanceof Boolean b) {
            return b;
        }

        if (any instanceof CharSequence cs) {
            String s = cs.toString().trim();

            if (s.isEmpty()) {
                return null;
            }

            if ("Y".equalsIgnoreCase(s) || "YES".equalsIgnoreCase(s) || "TRUE".equalsIgnoreCase(s)) {
                return Boolean.TRUE;
            }

            if ("N".equalsIgnoreCase(s) || "NO".equalsIgnoreCase(s) || "FALSE".equalsIgnoreCase(s))
                return Boolean.FALSE;
            try {
                return Integer.parseInt(s) != 0;
            } catch (NumberFormatException ignored) {
            }

            return null;
        }

        if (any instanceof Number n) {
            return n.intValue() != 0;
        }

        return null;
    }
}
