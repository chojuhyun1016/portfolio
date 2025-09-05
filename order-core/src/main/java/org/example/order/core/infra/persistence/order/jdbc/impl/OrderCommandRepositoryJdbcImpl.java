package org.example.order.core.infra.persistence.order.jdbc.impl;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.order.domain.order.entity.OrderEntity;
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
        String sql = """
                insert ignore into order (id, user_id, user_number, order_id, order_number, order_price,
                                            published_datetime, delete_yn, created_user_id, created_user_type,
                                            created_datetime, modified_user_id, modified_user_type, modified_datetime)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int[] argTypes = {
                Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR,
                Types.BIGINT, Types.TIMESTAMP, Types.TINYINT, Types.BIGINT, Types.VARCHAR,
                Types.TIMESTAMP, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP
        };

        List<Object[]> batchArgs = new ArrayList<>();

        entities.forEach(entity -> {
            if (entity.getId() == null) {
                entity.setId(tsidFactory.create().toLong());
            }

            batchArgs.add(new Object[]{
                    entity.getId(),
                    entity.getUserId(),
                    entity.getUserNumber(),
                    entity.getOrderId(),
                    entity.getOrderNumber(),
                    entity.getOrderPrice(),
                    entity.getPublishedDatetime(),
                    entity.getDeleteYn(),
                    entity.getCreatedUserId(),
                    entity.getCreatedUserType(),
                    entity.getCreatedDatetime(),
                    entity.getModifiedUserId(),
                    entity.getModifiedUserType(),
                    entity.getModifiedDatetime()
            });
        });

        for (int i = 0; i < batchArgs.size(); i += batchChunkSize) {
            int end = Math.min(i + batchChunkSize, batchArgs.size());
            List<Object[]> chunk = batchArgs.subList(i, end);

            jdbcTemplate.batchUpdate(sql, chunk, argTypes);

            if (log.isDebugEnabled()) {
                log.debug("bulkInsert chunk processed: {} - {}", i, end - 1);
            }
        }
    }

    @Override
    public void bulkUpdate(List<OrderUpdate> syncList) {
        String sql = """
                update order set user_id = ?,
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
                Types.TIMESTAMP, Types.TINYINT, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP,
                Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT, Types.TIMESTAMP
        };

        List<Object[]> batchArgs = new ArrayList<>();

        for (OrderUpdate sync : syncList) {
            batchArgs.add(new Object[]{
                    sync.userId(),
                    sync.userNumber(),
                    sync.orderId(),
                    sync.orderNumber(),
                    sync.orderPrice(),
                    sync.publishedDateTime(),
                    sync.deleteYn(),
                    sync.createdUserId(),
                    sync.createdUserType(),
                    sync.createdDatetime(),
                    sync.modifiedUserId(),
                    sync.modifiedUserType(),
                    sync.modifiedDatetime(),
                    sync.orderId(),
                    sync.publishedDateTime()
            });
        }

        for (int i = 0; i < batchArgs.size(); i += batchChunkSize) {
            int end = Math.min(i + batchChunkSize, batchArgs.size());
            List<Object[]> chunk = batchArgs.subList(i, end);

            jdbcTemplate.batchUpdate(sql, chunk, argTypes);

            if (log.isDebugEnabled()) {
                log.debug("bulkUpdate chunk processed: {} - {}", i, end - 1);
            }
        }
    }
}
