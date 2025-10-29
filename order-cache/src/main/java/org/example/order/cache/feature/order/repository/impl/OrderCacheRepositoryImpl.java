package org.example.order.cache.feature.order.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.cache.feature.order.key.OrderCacheKeys;
import org.example.order.cache.feature.order.model.OrderCacheRecord;
import org.example.order.cache.feature.order.repository.OrderCacheRepository;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Order Cache Repository 구현
 * ------------------------------------------------------------------------
 * 특징
 * - value 타입 관용 처리:
 * 1) OrderCacheRecord → 그대로 사용
 * 2) Map → 필드 매핑 후 표준 DTO로 "재저장"
 * 3) String(JSON) → Map 파싱 후 표준 DTO로 "재저장"
 * - 위 관용 처리는 "과거 캐시 값 호환"을 위한 일회성 정규화 목적
 * - 운영 기준: 키/TTL/버전/스키마 관리는 캐시 레이어에서만 수행
 */
@Slf4j
@RequiredArgsConstructor
public class OrderCacheRepositoryImpl implements OrderCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public Optional<OrderCacheRecord> get(Long orderId) {
        String key = OrderCacheKeys.orderKey(orderId);
        Object v = redisTemplate.opsForValue().get(key);

        if (v == null) {
            return Optional.empty();
        }

        // 남은 TTL 확보(초). -1: 무기한, -2: 없음
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // 1) 표준 DTO
        if (v instanceof OrderCacheRecord c) {
            return Optional.of(c);
        }

        // 2) Map (GenericJackson2JsonRedisSerializer가 타입정보 없이 역직렬화했을 때)
        if (v instanceof Map<?, ?> m) {
            OrderCacheRecord fixed = fromLooseMap(orderId, m);
            reSetWithTtl(key, fixed, ttl);

            return Optional.of(fixed);
        }

        // 3) String(JSON) — 보수적 처리
        if (v instanceof String s) {
            try {
                Map<String, Object> m = mapper.readValue(s, new TypeReference<Map<String, Object>>() {
                });

                OrderCacheRecord fixed = fromLooseMap(orderId, m);
                reSetWithTtl(key, fixed, ttl);

                return Optional.of(fixed);
            } catch (Exception e) {
                log.warn("[order-cache] JSON parse failed. key={} cause={}", key, e.toString());
            }
        }

        log.warn("[order-cache] Unsupported redis value type. key={} type={}", key, v.getClass().getName());

        return Optional.empty();
    }

    @Override
    public void put(OrderCacheRecord record, Long ttlSeconds) {
        String key = OrderCacheKeys.orderKey(record.orderId());

        if (ttlSeconds == null || ttlSeconds <= 0) {
            redisTemplate.opsForValue().set(key, record);
        } else {
            redisTemplate.opsForValue().set(key, record, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void evict(Long orderId) {
        redisTemplate.delete(OrderCacheKeys.orderKey(orderId));
    }

    /* ------------------------ 관용 매핑 ------------------------ */

    private static OrderCacheRecord fromLooseMap(Long orderId, Map<?, ?> m) {
        Object rawOrderId = m.containsKey("orderId") ? m.get("orderId") : orderId;
        Object rawOrderNumber = m.containsKey("orderNumber") ? m.get("orderNumber") : null;
        Object rawUserId = m.containsKey("userId") ? m.get("userId") : null;
        Object rawUserNumber = m.containsKey("userNumber") ? m.get("userNumber") : null;
        Object rawOrderPrice = m.containsKey("orderPrice") ? m.get("orderPrice") : null;
        Object rawVersion = m.containsKey("versionStamp") ? m.get("versionStamp") : 0L;

        Long orderIdVal = longOf(rawOrderId);
        String orderNumber = strOf(rawOrderNumber);
        Long userId = longOf(rawUserId);
        String userNumber = strOf(rawUserNumber);
        Long orderPrice = longOf(rawOrderPrice);
        Long versionStamp = longOf(rawVersion);

        // 누락 필드는 null 로 채움
        return new OrderCacheRecord(
                orderIdVal,
                orderNumber,
                userId,
                userNumber,
                orderPrice,
                null,    // deleteYn
                null,    // createdUserId
                null,    // createdUserType
                null,    // createdDatetime
                null,    // modifiedUserId
                null,    // modifiedUserType
                null,    // modifiedDatetime
                null,    // publishedTimestamp
                versionStamp
        );
    }

    private void reSetWithTtl(String key, OrderCacheRecord value, Long ttlSeconds) {
        try {
            if (ttlSeconds == null || ttlSeconds < 0) {
                // -1(무기한), null → 무기한으로 저장
                redisTemplate.opsForValue().set(key, value);
            } else if (ttlSeconds == 0) {
                // 0이면 일부 구현에서 즉시만료로 해석될 수 있으므로 무기한 처리
                redisTemplate.opsForValue().set(key, value);
            } else {
                redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("[order-cache] reSetWithTtl failed key={} cause={}", key, e.toString());

            redisTemplate.opsForValue().set(key, value);
        }
    }

    private static Long longOf(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Number n) {
            return n.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(x));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String strOf(Object x) {
        return x == null ? null : String.valueOf(x);
    }
}
