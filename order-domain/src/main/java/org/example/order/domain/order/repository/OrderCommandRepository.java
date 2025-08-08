package org.example.order.domain.order.repository;

import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;

import java.util.List;

/**
 * 커스텀 커맨드 Repository 인터페이스 (도메인)
 * - 대량 데이터 조작용 (Bulk Insert, Bulk Update 등)
 */
public interface OrderCommandRepository {
    void bulkInsert(List<OrderEntity> entities);

    void bulkUpdate(List<OrderUpdate> syncList);
}
