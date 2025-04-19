package org.example.order.core.infra.dynamo.port.in;

import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;

import java.util.List;
import java.util.Optional;

/**
 * port.in: 애플리케이션 레이어에서 유스케이스 실행을 위한 진입점. 보통 API, Scheduler, 이벤트 핸들러 등이 이 인터페이스를 사용.
 * port.out: 도메인 로직이 외부 시스템 (DB, 메시지 브로커 등)에 접근해야 할 때 사용하는 인터페이스. 즉, 어댑터가 구현함.

 * UseCase 관점의 포트 (Application Layer -> Domain Layer)
 * Order에 관련된 기능들을 정의합니다.
 */
public interface OrderDynamoUseCasePort {

    void registerOrder(OrderDynamoEntity entity);

    Optional<OrderDynamoEntity> getOrder(String id);

    List<OrderDynamoEntity> getAllOrders();

    List<OrderDynamoEntity> getOrdersByUserId(Long userId);
}
