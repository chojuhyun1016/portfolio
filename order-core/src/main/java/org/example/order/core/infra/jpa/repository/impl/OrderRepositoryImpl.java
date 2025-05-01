package org.example.order.core.infra.jpa.repository.impl;

import com.github.f4b6a3.tsid.TsidFactory;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.application.order.vo.OrderVo;
import org.example.order.core.infra.jpa.repository.CustomOrderRepository;
import org.example.order.core.infra.jpa.querydsl.builder.QuerydslUtils;
import org.example.order.core.domain.order.entity.OrderEntity;
import org.example.order.core.domain.order.entity.QOrderEntity;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static com.querydsl.core.group.GroupBy.groupBy;

@Repository
public class OrderRepositoryImpl extends QuerydslRepositorySupport implements CustomOrderRepository {
    private static final QOrderEntity ORDER = QOrderEntity.orderEntity;

    private final JPAQueryFactory queryFactory;
    private final JdbcTemplate jdbcTemplate;
    private final TsidFactory tsidFactory;

    public OrderRepositoryImpl(JPAQueryFactory queryFactory, JdbcTemplate jdbcTemplate, TsidFactory tsidFactory) {
        super(OrderEntity.class);
        this.queryFactory = queryFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.tsidFactory = tsidFactory;
    }

    @Override
    public OrderVo fetchByOrderId(Long orderId) {
        return queryFactory.from(ORDER)
                .where(ORDER.orderId.eq(orderId))
                .transform(groupBy(ORDER.orderId).as(
                        Projections.constructor(OrderVo.class, ORDER)
                )).get(orderId);
    }

    @Override
    @Transactional
    public void bulkInsert(List<OrderEntity> entities) {
        String sql = """
                insert ignore into order (id,
                                          user_id,
                                          user_number,
                                          order_id,
                                          order_number,
                                          order_price,
                                          published_datetime,
                                          delete_yn,
                                          created_user_id,
                                          created_user_type,
                                          created_datetime,
                                          modified_user_id,
                                          modified_user_type,
                                          modified_datetime) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        List<Object[]> batchArgs = new ArrayList<>();
        int[] args = {Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.TIMESTAMP, Types.TINYINT,
                      Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP};

        entities.forEach(entity -> entity.updateTsid(tsidFactory.create().toLong()));

        for (int i = 0; i < entities.size(); i += QuerydslUtils.DEFAULT_BATCH_SIZE) {
            int end = Math.min(entities.size(), i + QuerydslUtils.DEFAULT_BATCH_SIZE);
            List<OrderEntity> batchList = entities.subList(i, end);
            for (OrderEntity entity : batchList) {
                batchArgs.add(new Object[]{
                        entity.getId(),
                        entity.getOrderId(),
                        entity.getOrderNumber(),
                        entity.getUserId(),
                        entity.getUserNumber(),
                        entity.getOrderPrice(),
                        entity.getPublishedDatetime(),
                        entity.getDeleteYn(),
                        entity.getCreatedUserId(),
                        entity.getCreatedUserType(),
                        entity.getCreatedDatetime(),
                        entity.getModifiedUserId(),
                        entity.getModifiedUserType(),
                        entity.getModifiedDatetime(),
                        entity.getUserNumber()
                });
            }

            jdbcTemplate.batchUpdate(sql, batchArgs, args);
            batchArgs.clear();
        }
    }

    @Override
    public void bulkUpdate(List<OrderSyncCommand> dtoList) {
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
                                 version = version + 1 where order_id = ? and published_datetime <= ?
                """;

        int[] args = {Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.TIMESTAMP, Types.TINYINT,
                      Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT, Types.VARCHAR, Types.TIMESTAMP};

        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 0; i < dtoList.size(); i += QuerydslUtils.DEFAULT_BATCH_SIZE) {
            int end = Math.min(dtoList.size(), i + QuerydslUtils.DEFAULT_BATCH_SIZE);
            List<OrderSyncCommand> batchList = dtoList.subList(i, end);

            for (OrderSyncCommand dto : batchList) {
                batchArgs.add(new Object[]{
                        dto.getUserId(),
                        dto.getUserNumber(),
                        dto.getOrderId(),
                        dto.getOrderNumber(),
                        dto.getOrderPrice(),
                        dto.getPublishedDateTimeStr(),
                        dto.getDeleteYn(),
                        dto.getCreatedUserId(),
                        dto.getCreatedUserType(),
                        dto.getCreatedDatetime(),
                        dto.getModifiedUserId(),
                        dto.getModifiedUserType(),
                        dto.getModifiedDatetime()
                });
            }

            int[] execute = jdbcTemplate.batchUpdate(sql, batchArgs, args);
            for (int j = 0; j < execute.length; j++) {
                if (execute[j] == 0) {
                    batchList.get(j).fail();
                }
            }
            batchArgs.clear();
        }
    }
}
