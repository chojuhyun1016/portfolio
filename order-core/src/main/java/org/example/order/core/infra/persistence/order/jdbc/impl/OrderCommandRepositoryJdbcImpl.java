package org.example.order.core.infra.persistence.order.jdbc.impl;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderBatchOptions;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderCommandRepository 구현체 (JDBC)
 * <p>
 * - JpaInfraConfig 에서 jpa.enabled=true 일 때에만 조건부 등록
 */
@Slf4j
@RequiredArgsConstructor
public class OrderCommandRepositoryJdbcImpl implements OrderCommandRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TsidFactory tsidFactory;

    @Setter
    private int batchChunkSize = 10000;

    @Override
    @Transactional
    public void bulkInsert(List<OrderEntity> entities) {
        bulkInsert(entities, null);
    }

    @Override
    @Transactional
    public void bulkUpdate(List<OrderUpdate> syncList) {
        bulkUpdate(syncList, null);
    }

    @Override
    @Transactional
    public void bulkInsert(List<OrderEntity> entities, OrderBatchOptions options) {
        final int chunk = resolveChunk(options);

        String sql = """
                insert ignore into `order` (id, user_id, user_number, order_id, order_number, order_price,
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

        for (OrderEntity e : entities) {
            if (e.getId() == null) {
                e.setId(tsidFactory.create().toLong());
            }

            batchArgs.add(new Object[]{
                    e.getId(),
                    e.getUserId(),
                    e.getUserNumber(),
                    e.getOrderId(),
                    e.getOrderNumber(),
                    e.getOrderPrice(),
                    e.getPublishedDatetime(),
                    e.getDeleteYn(),          // Boolean → VARCHAR(1) 컬럼 (드라이버가 문자열/숫자로 처리 가능)
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

            if (log.isDebugEnabled()) {
                log.debug("jdbc_bulk op=insert chunk={} range=[{}, {})", chunk, i, end);
            }
        }
    }

    @Override
    @Transactional
    public void bulkUpdate(List<OrderUpdate> syncList, OrderBatchOptions options) {
        final int chunk = resolveChunk(options);

        String sql = """
                update `order` set user_id = ?,
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
                    s.deleteYn(),
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

            if (log.isDebugEnabled()) {
                log.debug("jdbc_bulk op=update chunk={} range=[{}, {})", chunk, i, end);
            }
        }
    }

    private int resolveChunk(OrderBatchOptions options) {
        if (options != null && options.getBatchChunkSize() != null && options.getBatchChunkSize() > 0) {
            return options.getBatchChunkSize();
        }

        return this.batchChunkSize;
    }
}
