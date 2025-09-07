package org.example.order.core.infra.persistence.order.jpa;

import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.example.order.core.infra.persistence.order.jpa.impl.OrderRepositoryJpaImpl;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 순수 단위 테스트:
 * - 스프링/DB/트랜잭션 미사용
 * - EntityManager/JPAQueryFactory 목킹
 */
class OrderRepositoryJpaImplTest {

    private final EntityManager em = mock(EntityManager.class);
    private final JPAQueryFactory qf = mock(JPAQueryFactory.class);
    private final OrderRepository repo = new OrderRepositoryJpaImpl(qf, em);

    private static OrderEntity newEntity(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity e = new OrderEntity();
        e.setUserId(10L);
        e.setUserNumber("U-10");
        e.setOrderId(orderId);
        e.setOrderNumber("O-" + orderId);
        e.setOrderPrice(1000L);
        e.setDeleteYn(false);
        e.setPublishedDatetime(now);
        e.setCreatedUserId(1L);
        e.setCreatedUserType("SYS");
        e.setCreatedDatetime(now);
        e.setModifiedUserId(1L);
        e.setModifiedUserType("SYS");
        e.setModifiedDatetime(now);
        e.setVersion(0L);
        return e;
    }

    @Test
    @DisplayName("save(): ID가 없으면 persist 호출")
    void save_persist_whenIdNull() {
        OrderEntity e = newEntity(1001L);
        e.setId(null);

        repo.save(e);

        verify(em, times(1)).persist(e);
        verify(em, never()).merge(any());
        verifyNoMoreInteractions(em);
    }

    @Test
    @DisplayName("save(): ID가 있으면 merge 호출")
    void save_merge_whenIdPresent() {
        OrderEntity e = newEntity(1002L);
        e.setId(123L);

        when(em.merge(e)).thenReturn(new OrderEntity());

        repo.save(e);

        verify(em, never()).persist(any());
        verify(em, times(1)).merge(e);
        verifyNoMoreInteractions(em);
    }

    @Test
    @DisplayName("deleteByOrderIdIn(): Querydsl delete 체인 NPE 없이 수행")
    void deleteByOrderIdIn_callsFlow() {
        List<Long> ids = List.of(1L, 2L, 3L);

        // delete 체인을 자기 자신 반환으로 설정
        JPADeleteClause delete = mock(JPADeleteClause.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
        when(qf.delete(any())).thenReturn(delete);
        when(delete.execute()).thenReturn(3L);

        repo.deleteByOrderIdIn(ids);

        // 체인 호출이 실제로 있었는지 명시 검증
        verify(qf, times(1)).delete(any());
        verify(delete, atLeastOnce()).where(any()); // where 호출 허용/검증
        verify(delete, times(1)).execute();

        // JPA persist/merge 등은 호출되지 않아야 함
        verify(em, never()).persist(any());
        verify(em, never()).merge(any());

        // delete에 대해 NoMoreInteractions 검증은 하지 않는다(체인 호출이 있기 때문)
        verifyNoMoreInteractions(em);
    }
}
