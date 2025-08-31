package org.example.order.core.infra.dynamo;

import org.example.order.core.infra.persistence.order.dynamo.impl.OrderDynamoRepositoryImpl;
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
 * Dynamo Repository 단위 테스트
 * <p>
 * - findById / deleteById key 빌더 확인
 * - findAll / findByUserId 스캔 체인 호출 검증
 */
class DynamoRepositoryTest {

    @Test
    void findById_builds_partition_key_and_calls_getItem() {
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);

        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);
        when(table.getItem(any(Consumer.class))).thenReturn(null);

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");
        repo.findById("id-123");

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
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);

        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);
        when(table.deleteItem(any(Consumer.class))).thenReturn(null);

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");
        repo.deleteById("del-999");

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
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);

        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);
        @SuppressWarnings("unchecked")
        PageIterable<OrderDynamoEntity> pageIterable = mock(PageIterable.class);
        when(table.scan()).thenReturn(pageIterable);

        List<OrderDynamoEntity> data = List.of(entity("a", 1L), entity("b", 2L), entity("c", 2L));
        when(pageIterable.items()).thenReturn(sdkIterableOf(data));

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");
        var all = repo.findAll();

        assertThat(all).extracting(OrderDynamoEntity::getId).containsExactlyInAnyOrder("a", "b", "c");
        verify(table, times(1)).scan();
        verify(pageIterable, times(1)).items();
    }

    @Test
    void findByUserId_triggers_scan_and_filters_in_memory() {
        DynamoDbEnhancedClient enhanced = mock();
        @SuppressWarnings("unchecked")
        DynamoDbTable<OrderDynamoEntity> table = mock(DynamoDbTable.class);

        when(enhanced.table(eq("order_dynamo"), any(TableSchema.class))).thenReturn(table);
        @SuppressWarnings("unchecked")
        PageIterable<OrderDynamoEntity> pageIterable = mock(PageIterable.class);
        when(table.scan()).thenReturn(pageIterable);

        List<OrderDynamoEntity> data = List.of(entity("o-1", 1L), entity("o-2", 2L), entity("o-3", 1L));
        when(pageIterable.items()).thenReturn(sdkIterableOf(data));

        OrderDynamoRepositoryImpl repo = new OrderDynamoRepositoryImpl(enhanced, "order_dynamo");
        var onlyU1 = repo.findByUserId(1L);

        assertThat(onlyU1).extracting(OrderDynamoEntity::getId).containsExactlyInAnyOrder("o-1", "o-3");
        verify(table, times(1)).scan();
        verify(pageIterable, times(1)).items();
    }

    private static OrderDynamoEntity entity(String id, Long userId) {
        OrderDynamoEntity e = new OrderDynamoEntity();
        e.setId(id);
        e.setUserId(userId);

        return e;
    }

    private static <T> SdkIterable<T> sdkIterableOf(List<T> list) {
        return new SdkIterable<>() {
            @Override
            public Iterator<T> iterator() {
                return list.iterator();
            }

            @Override
            public Stream<T> stream() {
                return list.stream();
            }
        };
    }
}
