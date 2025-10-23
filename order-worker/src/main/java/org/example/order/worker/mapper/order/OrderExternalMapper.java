package org.example.order.worker.mapper.order;

import org.example.order.core.application.order.dto.sync.LocalOrderSync;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.springframework.stereotype.Component;

/**
 * OrderExternalMapper
 * - 외부 동기화 매핑 전담
 * - LocalOrderSync -> OrderDynamoEntity
 * - Redis 키 생성 등은 서비스의 헬퍼로 유지
 * - Mapper vs Converter:
 * - Mapper: 타입 간 "필드 매핑" 중심 (여기)
 * - Converter: 포맷/단위/유효성/규칙 변환이 더 큰 경우
 */
@Component
public class OrderExternalMapper {

    public OrderDynamoEntity toDynamo(LocalOrderSync d) {
        if (d == null) {
            return null;
        }

        OrderDynamoEntity e = new OrderDynamoEntity();
        e.setId(String.valueOf(d.orderId()));
        e.setOrderId(d.orderId());
        e.setOrderNumber(d.orderNumber());
        e.setUserId(d.userId());
        e.setUserNumber(d.userNumber());
        e.setUserName(d.userNumber());
        e.setOrderPrice(d.orderPrice());
        e.setDeleteYn(Boolean.TRUE.equals(d.deleteYn()) ? "Y" : "N");
        e.setCreatedUserId(d.createdUserId());
        e.setCreatedUserType(d.createdUserType());
        e.setCreatedDatetime(d.createdDatetime());
        e.setModifiedUserId(d.modifiedUserId());
        e.setModifiedUserType(d.modifiedUserType());
        e.setModifiedDatetime(d.modifiedDatetime());
        e.setPublishedTimestamp(d.publishedTimestamp());

        return e;
    }
}
