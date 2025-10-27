package com.example.order.api.web.service.order.impl;

import com.example.order.api.web.service.order.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.application.order.dto.query.OrderQuery;
import org.example.order.core.application.order.dto.view.OrderView;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.example.order.core.infra.persistence.order.redis.RedisRepository;
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
    private final RedisRepository redisRepository;                 // Redis (일반 연산)
    private final OrderMapper orderMapper;                         // Entity/Sync/View 매퍼

    /**
     * MySQL(JPA) 조회
     * - 주의: 현재 Repository.findById(id)는 PK 기준입니다.
     * 운영에서 orderId가 PK가 아닐 경우, 별도 쿼리 메서드 필요합니다.
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

        // Entity -> View (Sync 생략 경로)
        return orderMapper.toView(entity);
    }

    /**
     * DynamoDB 조회
     * - OrderDynamoRepository는 파티션키(String id) 기준으로 조회
     * - 도메인 스키마에 따라 orderId->id 매핑 정책을 맞춰야 함
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

        // Dynamo 엔티티 필드 타입 보정:
        // - deleteYn: "Y"/"N" -> Boolean
        // - version: 엔티티에 없을 수 있으므로 null
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
     * Redis 조회
     * - 키 컨벤션: "order:{orderId}"
     * - 값 컨벤션: OrderView 또는 OrderEntity/OrderSync 직렬화 오브젝트
     * - 현 구현은 가장 단순하게 Value에 OrderView가 저장되어 있다고 가정
     * (운영에서는 Serializer 설정/스키마 정합성 보장 필요)
     */
    @Override
    @Transactional(readOnly = true)
    public OrderView findByRedis(OrderQuery query) {
        String key = "order:" + query.orderId();
        Object v = redisRepository.get(key);

        if (v == null) {
            String msg = "Order not found in Redis. key=" + key;
            log.warn("[OrderQueryService][Redis] {}", msg);

            throw new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
        }

        if (v instanceof OrderView view) {
            return view;
        }

        if (v instanceof OrderEntity entity) {
            return orderMapper.toView(entity);
        }

        // 그 외 타입(예: JSON 문자열/Map 등)은 상황에 맞게 역직렬화/매핑 구현 필요
        String msg = "Unsupported redis value type: " + v.getClass().getName();
        log.warn("[OrderQueryService][Redis] {}", msg);

        throw new CommonException(CommonExceptionCode.UNKNOWN_SERVER_ERROR, msg);
    }

    /* ------------------------ helpers ------------------------ */

    /**
     * "Y"/"N", Boolean, Number 등 다양한 표현을 Boolean으로 안전 변환
     */
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

            if ("N".equalsIgnoreCase(s) || "NO".equalsIgnoreCase(s) || "FALSE".equalsIgnoreCase(s)) {
                return Boolean.FALSE;
            }

            // 숫자 문자열 처리
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
