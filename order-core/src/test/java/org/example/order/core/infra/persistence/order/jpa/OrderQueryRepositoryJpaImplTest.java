//package org.example.order.core.infra.persistence.order.jpa;
//
//import com.querydsl.core.types.Expression;
//import com.querydsl.jpa.impl.JPAQuery;
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import org.example.order.core.infra.persistence.order.jpa.impl.OrderQueryRepositoryJpaImpl;
//import org.example.order.domain.order.model.OrderView;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Answers;
//import org.mockito.Mockito;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.*;
//
///**
// * 순수 단위 테스트 (스프링/JPA 미기동)
// * - Querydsl 오버로드 모호성 회피:
// * 1) select(...) 스텁 시 Expression<LocalOrderView>로 any 매처 캐스팅
// * 2) 체이닝(from/where/limit)은 RETURNS_SELF
// * 3) 불필요한 verifyNoMoreInteractions 제거(체인 호출도 상호작용으로 집계되기 때문)
// */
//class OrderQueryRepositoryJpaImplTest {
//
//    private final JPAQuery<OrderView> query =
//            Mockito.mock(JPAQuery.class, withSettings().defaultAnswer(Answers.RETURNS_SELF));
//
//    private final JPAQueryFactory qf = mock(JPAQueryFactory.class);
//
//    private final OrderQueryRepositoryJpaImpl repo = new OrderQueryRepositoryJpaImpl(qf);
//
//    @SuppressWarnings("unchecked")
//    private static Expression<OrderView> anyOrderViewExpr() {
//        return (Expression<OrderView>) any(Expression.class);
//    }
//
//    @Test
//    @DisplayName("fetchByOrderId(): 조회 결과 없으면 Optional.empty")
//    void fetchByOrderId_notFound() {
//        when(qf.select(anyOrderViewExpr())).thenReturn(query);
//        when(query.limit(anyLong())).thenReturn(query);
//        when(query.fetchFirst()).thenReturn(null);
//
//        Optional<OrderView> opt = repo.fetchByOrderId(123L);
//
//        assertThat(opt).isEmpty();
//        verify(query, times(1)).limit(1L);
//        verify(query, times(1)).fetchFirst();
//    }
//
//    @Test
//    @DisplayName("fetchByOrderId(): 존재하면 Optional.of")
//    void fetchByOrderId_found() {
//        OrderView view = new OrderView(123L, "O-123", 10L, "U-10", 1000L);
//
//        when(qf.select(anyOrderViewExpr())).thenReturn(query);
//        when(query.limit(anyLong())).thenReturn(query);
//        when(query.fetchFirst()).thenReturn(view);
//
//        Optional<OrderView> opt = repo.fetchByOrderId(123L);
//
//        assertThat(opt).contains(view);
//        verify(query, times(1)).limit(1L);
//        verify(query, times(1)).fetchFirst();
//    }
//}
