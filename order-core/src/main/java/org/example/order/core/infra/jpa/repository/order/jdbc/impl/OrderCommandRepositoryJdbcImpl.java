package org.example.order.core.infra.jpa.repository.order.jdbc.impl;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrderCommandRepositoryJdbcImpl implements OrderCommandRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TsidFactory tsidFactory;

    @Override
    @Transactional
    public void bulkInsert(List<OrderEntity> entities) {
        String sql = """
                insert ignore into `order` (id, user_id, user_number, order_id, order_number, order_price,
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

        jdbcTemplate.batchUpdate(sql, batchArgs, argTypes);
    }

    @Override
    public void bulkUpdate(List<OrderUpdate> syncList) {
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

        jdbcTemplate.batchUpdate(sql, batchArgs, argTypes);
    }
}
