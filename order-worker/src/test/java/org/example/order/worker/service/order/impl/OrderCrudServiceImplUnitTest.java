//package org.example.order.worker.service.order.impl;
//
//import org.example.order.core.application.order.dto.sync.LocalOrderSync;
//import org.example.order.core.application.order.mapper.OrderMapper;
//import org.example.order.domain.order.entity.OrderEntity;
//import org.example.order.domain.order.model.OrderUpdate;
//import org.example.order.domain.order.repository.OrderCommandRepository;
//import org.example.order.domain.order.repository.OrderQueryRepository;
//import org.example.order.domain.order.repository.OrderRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.dao.DataAccessException;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.*;
//
///**
// * CRUD 서비스: 매퍼/커맨드 위임 및 예외 전파만 단순 검증
// * 외부 동기화(Dynamo/Redis)는 내부 try/catch로 테스트에 영향 없음
// */
//class OrderCrudServiceImplUnitTest {
//
//    @Test
//    @DisplayName("bulkInsert: mapper → command.bulkInsert 호출")
//    void bulkInsert_delegates_to_command() {
//        OrderRepository repo = mock(OrderRepository.class);
//        OrderQueryRepository q = mock(OrderQueryRepository.class);
//        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
//        OrderMapper mapper = mock(OrderMapper.class);
//
//        OrderCrudServiceImpl sut = new OrderCrudServiceImpl(
//                repo, q, cmd, mapper, /* idGen */ null, /* dynamo */ null, /* redis */ null
//        );
//
//        LocalOrderSync d = new LocalOrderSync();
//        when(mapper.toEntity(d)).thenReturn(new OrderEntity());
//
//        sut.bulkInsert(List.of(d));
//
//        verify(mapper, times(1)).toEntity(d);
//        verify(cmd, times(1)).bulkInsert(anyList());
//    }
//
//    @Test
//    @DisplayName("bulkInsert: DataAccessException 전파")
//    void bulkInsert_throws_on_command_error() {
//        OrderRepository repo = mock(OrderRepository.class);
//        OrderQueryRepository q = mock(OrderQueryRepository.class);
//        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
//        OrderMapper mapper = mock(OrderMapper.class);
//
//        OrderCrudServiceImpl sut = new OrderCrudServiceImpl(
//                repo, q, cmd, mapper, null, null, null
//        );
//
//        when(mapper.toEntity(any())).thenReturn(new OrderEntity());
//        doThrow(new org.springframework.dao.DuplicateKeyException("dup"))
//                .when(cmd).bulkInsert(anyList());
//
//        assertThatThrownBy(() -> sut.bulkInsert(List.of(new LocalOrderSync())))
//                .isInstanceOf(DataAccessException.class);
//    }
//
//    @Test
//    @DisplayName("bulkUpdate: mapper.toUpdateCommands → command.bulkUpdate 호출")
//    void bulkUpdate_delegates_to_command() {
//        OrderRepository repo = mock(OrderRepository.class);
//        OrderQueryRepository q = mock(OrderQueryRepository.class);
//        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
//        OrderMapper mapper = mock(OrderMapper.class);
//
//        OrderCrudServiceImpl sut = new OrderCrudServiceImpl(
//                repo, q, cmd, mapper, null, null, null
//        );
//
//        when(mapper.toUpdateCommands(anyList())).thenReturn(List.of(mock(OrderUpdate.class)));
//
//        sut.bulkUpdate(List.of(new LocalOrderSync()));
//
//        verify(mapper, times(1)).toUpdateCommands(anyList());
//        verify(cmd, times(1)).bulkUpdate(anyList());
//    }
//
//    @Test
//    @DisplayName("deleteAll: repo.deleteByOrderIdIn 호출")
//    void deleteAll_delegates_to_repo() {
//        OrderRepository repo = mock(OrderRepository.class);
//        OrderQueryRepository q = mock(OrderQueryRepository.class);
//        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
//        OrderMapper mapper = mock(OrderMapper.class);
//
//        OrderCrudServiceImpl sut = new OrderCrudServiceImpl(
//                repo, q, cmd, mapper, null, null, null
//        );
//
//        LocalOrderSync d1 = new LocalOrderSync();
//        d1.setOrderId(1L);
//        LocalOrderSync d2 = new LocalOrderSync();
//        d2.setOrderId(2L);
//
//        sut.deleteAll(List.of(d1, d2));
//
//        verify(repo, times(1)).deleteByOrderIdIn(List.of(1L, 2L));
//    }
//}
