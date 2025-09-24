package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.core.infra.persistence.order.redis.RedisRepository;
import org.example.order.domain.common.id.IdGenerator;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderDynamoRepository;
import org.example.order.domain.order.repository.OrderQueryRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.example.order.worker.service.order.OrderCrudService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCrudServiceImpl implements OrderCrudService {

    private final OrderRepository orderRepository;                   // JPA 저장소 (증폭 INSERT/DELETE)
    private final OrderQueryRepository orderQueryRepository;         // QueryDSL 기반 조회/갱신(증폭 UPDATE)
    private final OrderCommandRepository orderCommandRepository;     // JDBC 벌크 커맨드 (원본 INSERT/UPDATE)

    private final OrderMapper orderMapper;
    private final IdGenerator idGenerator;

    private final OrderDynamoRepository orderDynamoRepository;
    private final RedisRepository redisRepository;

    private static final String REDIS_KEY_FMT = "order:byId:%s";

    private static final long ORDER_ID_OFFSET = 1_000_000_000_000L;
    private static final long ORDER_PRICE_DELTA = 5_000L;
    private static final String ORDER_NUMBER_SUFFIX = "-JPA";

    @Override
    public List<OrderEntity> bulkInsert(List<LocalOrderDto> dtoList) {
        try {
            // 1) DTO -> Entity (원본) & ID 보정
            List<OrderEntity> entities = dtoList.stream()
                    .map(orderMapper::toEntity)
                    .peek(e -> {
                        if (e.getId() == null) {
                            e.setId(idGenerator.nextId());
                        }
                    })
                    .toList();

            // 2) JDBC 벌크 INSERT (원본)
            orderCommandRepository.bulkInsert(entities);

            // 3) 증폭 INSERT — JPA로 새 PK 행 INSERT (항상 수행)
            for (OrderEntity src : entities) {
                try {
                    OrderEntity amplified = amplifyToEntity(src);

                    orderRepository.save(amplified);
                } catch (Throwable t) {
                    log.warn("[JPA][AMPLIFY][SKIP] orderId={} cause={}", src.getOrderId(), t.toString());
                }
            }

            // 4) 커밋 후 외부 동기화(원본만)
            afterCommit(() -> upsertExternal(dtoList));

            return entities;
        } catch (DataAccessException e) {
            log.error("error : OrderCrudEntity bulkInsert failed - msg : {}, cause : {}", e.getMessage(), e.getCause(), e);

            throw e;
        } catch (Exception e) {
            log.error("error : OrderCrudEntity bulkInsert failed", e);

            throw e;
        }
    }

    @Override
    public void bulkUpdate(List<LocalOrderDto> dtoList) {
        // 1) 원본은 JDBC 벌크 UPDATE
        List<OrderUpdate> commandList = orderMapper.toUpdateCommands(dtoList);

        orderCommandRepository.bulkUpdate(commandList);

        // 2) 증폭 UPDATE — 대상이 없으면 0건 유지(보완 INSERT 금지)
        for (LocalOrderDto d : dtoList) {
            try {
                if (d == null || d.getOrderId() == null) {
                    continue;
                }

                final long amplifiedOrderId = d.getOrderId() + ORDER_ID_OFFSET;
                final String amplifiedOrderNumber = (d.getOrderNumber() == null)
                        ? ORDER_NUMBER_SUFFIX
                        : (d.getOrderNumber() + ORDER_NUMBER_SUFFIX);
                final Long newPrice = ((d.getOrderPrice() == null) ? 0L : d.getOrderPrice()) + ORDER_PRICE_DELTA;

                orderQueryRepository.updateByOrderId(
                        amplifiedOrderId,
                        amplifiedOrderNumber,
                        d.getUserId(),
                        d.getUserNumber(),
                        newPrice,
                        d.getDeleteYn(),
                        d.getModifiedUserId(),
                        d.getModifiedUserType(),
                        d.getModifiedDatetime()
                );
            } catch (Throwable t) {
                log.error("[JPA][AMPLIFY][UPDATE][SKIP] orderId={} cause={}", d == null ? "null" : d.getOrderId(), t.toString());
            }
        }

        // 3) 커밋 후 외부 동기화(원본만)
        afterCommit(() -> upsertExternal(dtoList));
    }

    @Override
    public void deleteAll(List<LocalOrderDto> dtoList) {
        // 1) 원본 삭제
        List<Long> ids = dtoList.stream()
                .map(LocalOrderDto::getOrderId)
                .toList();

        orderRepository.deleteByOrderIdIn(ids);

        // 2) 증폭 삭제: orderId + OFFSET
        List<Long> amplifiedIds = new ArrayList<>(ids.size());

        for (Long id : ids) {
            if (id != null) {
                amplifiedIds.add(id + ORDER_ID_OFFSET);
            }
        }

        if (!amplifiedIds.isEmpty()) {
            try {
                orderRepository.deleteByOrderIdIn(amplifiedIds);
            } catch (Throwable t) {
                log.error("[JPA][AMPLIFY][DELETE][SKIP] ids={} cause={}", amplifiedIds, t.toString());
            }
        }

        // 3) 커밋 후 외부 삭제 동기화(원본만)
        afterCommit(() -> deleteExternal(dtoList));
    }

    /* ========================= 외부 동기화(원본만) ========================= */

    private void upsertExternal(List<LocalOrderDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // DynamoDB upsert
        for (LocalOrderDto d : items) {
            try {
                if (d == null || d.getOrderId() == null) {
                    continue;
                }

                orderDynamoRepository.save(toDynamo(d));
            } catch (Throwable t) {
                log.error("[DYNAMO][UPSERT][SKIP] orderId={} cause={}", safeId(d), t.toString());
            }
        }

        // Redis upsert
        for (LocalOrderDto d : items) {
            try {
                if (d == null || d.getOrderId() == null) {
                    continue;
                }

                String key = redisKey(d.getOrderId());

                redisRepository.set(key, d);
            } catch (Throwable t) {
                log.error("[REDIS][UPSERT][SKIP] orderId={} cause={}", safeId(d), t.toString());
            }
        }
    }

    private void deleteExternal(List<LocalOrderDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // DynamoDB delete
        for (LocalOrderDto d : items) {
            try {
                if (d == null || d.getOrderId() == null) {
                    continue;
                }

                final String id = String.valueOf(d.getOrderId());

                if (d.getOrderNumber() != null && !d.getOrderNumber().isBlank()) {
                    orderDynamoRepository.deleteByIdAndOrderNumber(id, d.getOrderNumber());
                } else {
                    orderDynamoRepository.deleteById(id);
                }
            } catch (Throwable t) {
                log.error("[DYNAMO][DELETE][SKIP] orderId={} cause={}", safeId(d), t.toString());
            }
        }

        // Redis delete
        for (LocalOrderDto d : items) {
            try {
                if (d == null || d.getOrderId() == null) {
                    continue;
                }

                redisRepository.delete(redisKey(d.getOrderId()));
            } catch (Throwable t) {
                log.error("[REDIS][DELETE][SKIP] orderId={} cause={}", safeId(d), t.toString());
            }
        }
    }

    /* ========================= 증폭(JPA INSERT) 헬퍼 ========================= */

    /**
     * 원본 엔티티를 변형하여 “새 PK”로 INSERT할 증폭 엔티티 생성
     */
    private OrderEntity amplifyToEntity(OrderEntity src) {
        OrderEntity e = new OrderEntity();

        // 새 PK (INSERT 보장)
        e.setId(idGenerator.nextId());

        // 업무키/컬럼 변형
        if (src.getOrderId() != null) {
            e.setOrderId(src.getOrderId() + ORDER_ID_OFFSET);
        } else {
            e.setOrderId(ORDER_ID_OFFSET);
        }

        if (src.getOrderNumber() != null) {
            e.setOrderNumber(src.getOrderNumber() + ORDER_NUMBER_SUFFIX);
        } else {
            e.setOrderNumber(ORDER_NUMBER_SUFFIX);
        }

        e.setUserId(src.getUserId());
        e.setUserNumber(src.getUserNumber());

        Long price = src.getOrderPrice();
        e.setOrderPrice((price == null ? 0L : price) + ORDER_PRICE_DELTA);

        e.setDeleteYn(src.getDeleteYn());
        e.setCreatedUserId(src.getCreatedUserId());
        e.setCreatedUserType(src.getCreatedUserType());
        e.setCreatedDatetime(src.getCreatedDatetime());
        e.setModifiedUserId(src.getModifiedUserId());
        e.setModifiedUserType(src.getModifiedUserType());
        e.setModifiedDatetime(src.getModifiedDatetime());
        e.setPublishedDatetime(src.getPublishedDatetime());

        return e;
    }

    /* ========================= 기타 헬퍼 ========================= */

    private static String redisKey(Long orderId) {
        return String.format(REDIS_KEY_FMT, String.valueOf(orderId));
    }

    private static String safeId(LocalOrderDto d) {
        return (d == null || d.getOrderId() == null) ? "null" : String.valueOf(d.getOrderId());
    }

    /**
     * LocalOrderDto → Dynamo 저장용 매핑
     */
    private static OrderDynamoEntity toDynamo(LocalOrderDto d) {
        OrderDynamoEntity e = new OrderDynamoEntity();
        e.setId(String.valueOf(d.getOrderId()));
        e.setOrderId(d.getOrderId());
        e.setOrderNumber(d.getOrderNumber());
        e.setUserId(d.getUserId());
        e.setUserNumber(d.getUserNumber());
        e.setOrderPrice(d.getOrderPrice());
        e.setDeleteYn(Boolean.TRUE.equals(d.getDeleteYn()) ? "Y" : "N");
        e.setCreatedUserId(d.getCreatedUserId());
        e.setCreatedUserType(d.getCreatedUserType());
        e.setCreatedDatetime(d.getCreatedDatetime());
        e.setModifiedUserId(d.getModifiedUserId());
        e.setModifiedUserType(d.getModifiedUserType());
        e.setModifiedDatetime(d.getModifiedDatetime());
        e.setPublishedTimestamp(d.getPublishedTimestamp());

        return e;
    }

    /**
     * 트랜잭션 커밋 이후 실행 유틸
     */
    private static void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                r.run();
            } catch (Throwable ignore) {
            }

            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    r.run();
                } catch (Throwable ignore) {
                }
            }
        });
    }
}
