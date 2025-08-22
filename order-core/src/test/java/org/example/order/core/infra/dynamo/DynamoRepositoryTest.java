package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.dynamo.repository.impl.OrderDynamoRepositoryImpl;
import org.example.order.domain.order.entity.OrderDynamoEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderDynamoRepositoryImpl 가 Enhanced-Client 람다 오버로드들을
 * 올바르게 조립해 호출하는지 "호출 모양"만 검증하는 유닛 테스트.
 * (네트워크/로컬스택 등 외부 의존 없음)
 */
class DynamoRepositoryTest {

    @Test
    void findById_builds_partition_key_and_calls_getItem() {
        // given
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);
        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);

        // getItem(Consumer<...>) 오버로드를 스텁 (null 반환 = Optional.empty())
        when(table.getItem(any(Consumer.class))).thenReturn(null);

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");

        // when
        repo.findById("id-123");

        // then - Consumer 를 캡처하여 실제로 구성된 Key 를 확인
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<GetItemEnhancedRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        verify(table, times(1)).getItem(captor.capture());

        GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
        captor.getValue().accept(builder);
        GetItemEnhancedRequest built = builder.build();

        assertThat(built.key()).isNotNull();
        assertThat(built.key().partitionKeyValue().s()).isEqualTo("id-123");
    }

    @Test
    void deleteById_builds_partition_key_and_calls_deleteItem() {
        // given
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);
        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);

        // ❗ deleteItem(Consumer<...>) 은 void 가 아님 → doNothing 금지
        //    null 을 반환하도록 스텁
        when(table.deleteItem(any(Consumer.class))).thenReturn(null);

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");

        // when
        repo.deleteById("del-999");

        // then - Consumer 캡처해 Key 검증
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DeleteItemEnhancedRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        verify(table, times(1)).deleteItem(captor.capture());

        DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
        captor.getValue().accept(builder);
        DeleteItemEnhancedRequest built = builder.build();

        assertThat(built.key()).isNotNull();
        assertThat(built.key().partitionKeyValue().s()).isEqualTo("del-999");
    }

    @Test
    void findAll_triggers_scan_items_stream_chain() {
        // given
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);
        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);

        @SuppressWarnings("unchecked")
        PageIterable<OrderDynamoEntity> pageIterable = mock(PageIterable.class);

        // scan() → PageIterable
        when(table.scan()).thenReturn(pageIterable);

        // PageIterable.items() → SdkIterable (stream() 가능한 구현을 돌려준다)
        List<OrderDynamoEntity> data = List.of(
                entity("a", 1L), entity("b", 2L), entity("c", 2L)
        );
        when(pageIterable.items()).thenReturn(sdkIterableOf(data));

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");

        // when
        var all = repo.findAll();

        // then
        assertThat(all).extracting(OrderDynamoEntity::getId).containsExactlyInAnyOrder("a", "b", "c");
        verify(table, times(1)).scan();
        verify(pageIterable, times(1)).items();
    }

    @Test
    void findByUserId_triggers_scan_and_filters_in_memory() {
        // given
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);
        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);

        @SuppressWarnings("unchecked")
        PageIterable<OrderDynamoEntity> pageIterable = mock(PageIterable.class);
        when(table.scan()).thenReturn(pageIterable);

        List<OrderDynamoEntity> data = List.of(
                entity("o-1", 1L), entity("o-2", 2L), entity("o-3", 1L)
        );
        when(pageIterable.items()).thenReturn(sdkIterableOf(data));

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");

        // when
        var onlyU1 = repo.findByUserId(1L);

        // then
        assertThat(onlyU1).extracting(OrderDynamoEntity::getId).containsExactlyInAnyOrder("o-1", "o-3");
        verify(table, times(1)).scan();
        verify(pageIterable, times(1)).items();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 헬퍼들
    // ─────────────────────────────────────────────────────────────────────

    private static OrderDynamoEntity entity(String id, Long userId) {
        OrderDynamoEntity e = new OrderDynamoEntity();
        e.setId(id);
        e.setUserId(userId);
        return e;
    }

    /**
     * PageIterable.items() 이 기대하는 SdkIterable 구현체.
     * Mockito mock 으로 stream() 까지 올바르게 흉내내기 까다로워
     * 간단한 익명 클래스로 실제 동작을 제공.
     */
    private static <T> SdkIterable<T> sdkIterableOf(List<T> list) {
        return new SdkIterable<>() {
            @Override public Iterator<T> iterator() { return list.iterator(); }
            @Override public Stream<T> stream()     { return list.stream();   }
        };
    }
}
