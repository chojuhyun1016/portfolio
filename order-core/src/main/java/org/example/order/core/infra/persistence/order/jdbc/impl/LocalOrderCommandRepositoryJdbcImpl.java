package org.example.order.core.infra.persistence.order.jdbc.impl;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.order.domain.order.entity.LocalOrderEntity;
import org.example.order.domain.order.model.OrderBatchOptions;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.LocalOrderCommandRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * LocalOrderCommandRepository 구현체 (JDBC)
 * - 기존 OrderCommandRepositoryJdbcImpl과 동일한 로직, 테이블명만 local_order
 */
@RequiredArgsConstructor
public class LocalOrderCommandRepositoryJdbcImpl implements LocalOrderCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    @Setter
    private int batchChunkSize = 10000;

    private static String yn(Boolean b) {
        return (b != null && b) ? "Y" : "N";
    }

    @Override
    public void bulkInsert(List<LocalOrderEntity> entities) {
        bulkInsert(entities, null);
    }

    @Override
    public void bulkUpdate(List<OrderUpdate> syncList) {
        bulkUpdate(syncList, null);
    }

    @Override
    public void bulkInsert(List<LocalOrderEntity> entities, OrderBatchOptions options) {
        final int chunk = resolveChunk(options);

        String sql = """
                insert ignore into `local_order` (id, user_id, user_number, order_id, order_number, order_price,
                                                 published_datetime, delete_yn, created_user_id, created_user_type,
                                                 created_datetime, modified_user_id, modified_user_type, modified_datetime, version)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int[] argTypes = {
                Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR,
                Types.BIGINT, Types.TIMESTAMP, Types.VARCHAR, Types.BIGINT, Types.VARCHAR,
                Types.TIMESTAMP, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT
        };

        List<Object[]> batchArgs = new ArrayList<>();

        for (LocalOrderEntity e : entities) {
            if (e.getId() == null) {
                throw new IllegalArgumentException("LocalOrderEntity.id must be assigned before bulkInsert");
            }

            batchArgs.add(new Object[]{
                    e.getId(),
                    e.getUserId(),
                    e.getUserNumber(),
                    e.getOrderId(),
                    e.getOrderNumber(),
                    e.getOrderPrice(),
                    e.getPublishedDatetime(),
                    yn(e.getDeleteYn()),
                    e.getCreatedUserId(),
                    e.getCreatedUserType(),
                    e.getCreatedDatetime(),
                    e.getModifiedUserId(),
                    e.getModifiedUserType(),
                    e.getModifiedDatetime(),
                    e.getVersion()
            });
        }

        for (int i = 0; i < batchArgs.size(); i += chunk) {
            int end = Math.min(i + chunk, batchArgs.size());

            jdbcTemplate.batchUpdate(sql, batchArgs.subList(i, end), argTypes);
        }
    }

    @Override
    public void bulkUpdate(List<OrderUpdate> syncList, OrderBatchOptions options) {
        final int chunk = resolveChunk(options);

        String sql = """
                update `local_order` set user_id = ?,
                                         user_number = ?,
                                         order_id = ?,
                                         order_number = ?,
                                         order_price = ?,
                                         published_datetime = ?,
                                         delete_yn = ?,
                                         created_user_id = ?,
                                         created_user_type = ?,
                                         created_datetime = ?,
                                         modified_user_id = ?,
                                         modified_user_type = ?,
                                         modified_datetime = ?,
                                         version = version + 1
                where order_id = ? and published_datetime <= ?
                """;

        int[] argTypes = {
                Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.BIGINT,
                Types.TIMESTAMP, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP,
                Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT, Types.TIMESTAMP
        };

        List<Object[]> batchArgs = new ArrayList<>();

        for (OrderUpdate s : syncList) {
            batchArgs.add(new Object[]{
                    s.userId(),
                    s.userNumber(),
                    s.orderId(),
                    s.orderNumber(),
                    s.orderPrice(),
                    s.publishedDateTime(),
                    yn(s.deleteYn()),
                    s.createdUserId(),
                    s.createdUserType(),
                    s.createdDatetime(),
                    s.modifiedUserId(),
                    s.modifiedUserType(),
                    s.modifiedDatetime(),
                    s.orderId(),
                    s.publishedDateTime()
            });
        }

        for (int i = 0; i < batchArgs.size(); i += chunk) {
            int end = Math.min(i + chunk, batchArgs.size());

            jdbcTemplate.batchUpdate(sql, batchArgs.subList(i, end), argTypes);
        }
    }

    private int resolveChunk(OrderBatchOptions options) {
        if (options != null && options.getBatchChunkSize() != null && options.getBatchChunkSize() > 0) {
            return options.getBatchChunkSize();
        }

        return this.batchChunkSize;
    }
}
