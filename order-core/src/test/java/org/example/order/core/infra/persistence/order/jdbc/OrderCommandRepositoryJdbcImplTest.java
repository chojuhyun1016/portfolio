package org.example.order.core.infra.persistence.order.jdbc;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import org.example.order.core.infra.persistence.order.jdbc.impl.OrderCommandRepositoryJdbcImpl;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderBatchOptions;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 순수 단위 테스트 (스프링/DB 미사용)
 * - 실제 구현 SQL 오버로드와 파라미터 개수에 맞춤
 *   - insert: 15개
 *   - update: 13개 SET + WHERE 2개 = 15개 (version은 version+1이라 바인딩 없음)
 * - verify: batchUpdate(sql, batchArgs, argTypes) 형태로 검증
 */
class OrderCommandRepositoryJdbcImplTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TsidFactory tsidFactory = mock(TsidFactory.class);

    private OrderCommandRepository newRepo() {
        return new OrderCommandRepositoryJdbcImpl(jdbcTemplate, tsidFactory);
    }

    private static OrderEntity sampleEntity(long orderId, long price, LocalDateTime t) {
        OrderEntity e = OrderEntity.createEmpty();
        e.setId(123L);
        e.updateAll(
                1L, "U-1",
                orderId, "O-" + orderId,
                price, false, 0L,
                t,
                1L, "SYS", t,
                2L, "SYS", t
        );
        return e;
    }

    @Test
    @DisplayName("bulkInsert(): JdbcTemplate.batchUpdate(sql, batchArgs, argTypes) — 파라미터 15개")
    void bulkInsert_callsBatchUpdate() {
        // given
        when(tsidFactory.create()).thenReturn(Tsid.from(100L), Tsid.from(101L), Tsid.from(102L));

        LocalDateTime base = LocalDateTime.now();
        List<OrderEntity> batch = new ArrayList<>();
        batch.add(sampleEntity(2000L, 1111L, base));
        batch.add(sampleEntity(2001L, 2222L, base));
        batch.add(sampleEntity(2002L, 3333L, base));

        OrderBatchOptions opt = OrderBatchOptions.builder().batchChunkSize(1000).build();

        // when
        newRepo().bulkInsert(batch, opt);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), argsCaptor.capture(), any(int[].class));

        List<Object[]> sent = argsCaptor.getValue();
        assertThat(sent).hasSize(3);

        // INSERT는 15개 바인딩(id 포함, version 포함)
        for (Object[] row : sent) {
            assertThat(row).hasSize(15);
        }
    }

    @Test
    @DisplayName("bulkUpdate(): JdbcTemplate.batchUpdate(sql, batchArgs, argTypes) — 파라미터 15개")
    void bulkUpdate_callsBatchUpdate() {
        // given
        LocalDateTime t2 = LocalDateTime.now();

        List<OrderUpdate> updates = List.of(
                new OrderUpdate(
                        1L, "U-1",
                        2000L, "O-2000",
                        2222L,
                        t2, false,
                        1L, "SYS", t2,
                        2L, "SYS", t2
                ),
                new OrderUpdate(
                        1L, "U-1",
                        2001L, "O-2001",
                        3333L,
                        t2, false,
                        1L, "SYS", t2,
                        2L, "SYS", t2
                )
        );

        OrderBatchOptions opt = OrderBatchOptions.builder().batchChunkSize(1000).build();

        // when
        newRepo().bulkUpdate(updates, opt);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), argsCaptor.capture(), any(int[].class));

        List<Object[]> rows = argsCaptor.getValue();
        assertThat(rows).hasSize(2);

        // UPDATE는 SET 13개 + WHERE 2개 = 총 15개 바인딩
        for (Object[] row : rows) {
            assertThat(row).hasSize(15);
        }
    }
}
