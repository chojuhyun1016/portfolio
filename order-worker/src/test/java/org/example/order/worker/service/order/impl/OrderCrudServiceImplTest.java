package org.example.order.worker.service.order.impl;

import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CRUD 서비스 위임 및 예외 전파 검증:
 * - bulkInsert / bulkUpdate / deleteAll
 */
class OrderCrudServiceImplTest {

    @Test
    @DisplayName("bulkInsert: mapper → command.bulkInsert 호출")
    void bulkInsert_success() {
        OrderRepository repo = mock(OrderRepository.class);
        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
        OrderMapper mapper = mock(OrderMapper.class);
        OrderCrudServiceImpl svc = new OrderCrudServiceImpl(repo, cmd, mapper);

        LocalOrderDto d = mock(LocalOrderDto.class);
        OrderEntity e = mock(OrderEntity.class);
        when(mapper.toEntity(d)).thenReturn(e);

        svc.bulkInsert(List.of(d));

        verify(mapper, times(1)).toEntity(d);
        verify(cmd, times(1)).bulkInsert(anyList());
    }

    @Test
    @DisplayName("bulkInsert: DataAccessException 재던지기")
    void bulkInsert_data_access_failure() {
        OrderRepository repo = mock(OrderRepository.class);
        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
        OrderMapper mapper = mock(OrderMapper.class);
        OrderCrudServiceImpl svc = new OrderCrudServiceImpl(repo, cmd, mapper);

        LocalOrderDto d = mock(LocalOrderDto.class);
        when(mapper.toEntity(any())).thenReturn(mock(OrderEntity.class));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
                .when(cmd).bulkInsert(anyList());

        assertThatThrownBy(() -> svc.bulkInsert(List.of(d)))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("bulkUpdate: mapper.toUpdateCommands → command.bulkUpdate 호출")
    void bulkUpdate_success() {
        OrderRepository repo = mock(OrderRepository.class);
        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
        OrderMapper mapper = mock(OrderMapper.class);
        OrderCrudServiceImpl svc = new OrderCrudServiceImpl(repo, cmd, mapper);

        when(mapper.toUpdateCommands(anyList()))
                .thenReturn(List.of(mock(OrderUpdate.class)));

        svc.bulkUpdate(List.of(mock(LocalOrderDto.class)));

        verify(mapper, times(1)).toUpdateCommands(anyList());
        verify(cmd, times(1)).bulkUpdate(anyList());
    }

    @Test
    @DisplayName("deleteAll: repo.deleteByOrderIdIn 호출")
    void deleteAll_success() {
        OrderRepository repo = mock(OrderRepository.class);
        OrderCommandRepository cmd = mock(OrderCommandRepository.class);
        OrderMapper mapper = mock(OrderMapper.class);
        OrderCrudServiceImpl svc = new OrderCrudServiceImpl(repo, cmd, mapper);

        LocalOrderDto d1 = mock(LocalOrderDto.class);
        LocalOrderDto d2 = mock(LocalOrderDto.class);
        when(d1.getOrderId()).thenReturn(1L);
        when(d2.getOrderId()).thenReturn(2L);

        svc.deleteAll(List.of(d1, d2));

        verify(repo, times(1)).deleteByOrderIdIn(List.of(1L, 2L));
    }
}
